"""챗봇 액션 메인 로직"""
import json
import logging
import re
from typing import Optional

from openai import OpenAI

from app.config import settings
from app.action import approval_client, form_builder, state
from app.action.schema import ActionRequest, ActionResponse, ActionState, ActionButton
from app.core.openai import client as openai_client
from app.core.pinecone import search_vectors
from app.core.openai import get_embedding
from datetime import date, timedelta

logger = logging.getLogger(__name__)


# ============================================
# 진입점
# ============================================

async def handle_action(
        request: ActionRequest,
        company_id: str,
        member_id: str,
        member_position_id: str) -> ActionResponse:
    """챗봇 액션 메인 핸들러"""
    logger.info(f"[action] ========================================")
    logger.info(
        f"[action] question='{request.question}', "
        f"session_id={request.session_id}, action={request.action}"
    )

    # 1. 사용자가 버튼 누른 경우 (action 필드)
    if request.action and request.session_id:
        return await _handle_user_action(request, company_id, member_id, member_position_id)

    # 2. 신규 또는 진행 중 세션
    current_state = _load_or_create_state(
        request.session_id, company_id, member_id, member_position_id
    )

    # 3. 양식 매칭 (첫 호출 또는 양식 미매칭 상태)
    if not current_state.document_id:
        document = await _match_form(request.question, company_id)
        if not document:
            return _error_response(
                "어떤 결재 양식을 작성하시려는지 명확히 알려주세요. "
                "예: '연차신청서', '출장신청'",
                current_state.session_id
            )
        current_state.document_id = document["documentId"]
        current_state.document_name = document["documentName"]
        current_state.form_schema = document["formSchema"]
        current_state.request_type = document.get("requestType")

    # 4. Function Calling으로 슬롯 추출
    extracted = await _extract_slots(
        question=request.question,
        history=request.conversation_history,
        existing_slots=current_state.slots,
        form_schema=current_state.form_schema,
        document_name=current_state.document_name
    )
    if extracted:
        current_state.slots.update(extracted)

    # 5. 부족한 필수 필드 확인
    missing = form_builder.find_missing_required_fields(
        current_state.slots, current_state.form_schema
    )

    if missing:
        state.save_state(current_state)
        return _ask_response(current_state, missing)

    # 6. 모든 필드 채워짐 → 확인 단계
    state.save_state(current_state)
    return _confirm_response(current_state)


# ============================================
# 사용자 액션 처리 (버튼 클릭)
# ============================================

async def _handle_user_action(
        request: ActionRequest,
        company_id: str,
        member_id: str,
        member_position_id: str) -> ActionResponse:
    """사용자가 버튼 누른 경우 처리"""
    current_state = state.load_state(request.session_id)

    if not current_state:
        return _error_response("세션이 만료되었습니다. 다시 시작해주세요.", None)

    if request.action == "without_attachment":
        return await _submit_request(current_state, status="WAIT")

    elif request.action == "with_attachment":
        return await _submit_request(current_state, status="DRAFT")

    elif request.action == "cancel":
        state.clear_state(request.session_id)
        return ActionResponse(
            type="cancelled",
            message="작성이 취소되었습니다.",
            session_id=request.session_id
        )

    else:
        return _error_response(f"알 수 없는 액션: {request.action}", request.session_id)


# ============================================
# 양식 매칭 (RAG)
# ============================================

async def _match_form(question: str, company_id: str) -> Optional[dict]:
    """RAG로 양식 매칭 후 approval-service에서 최신 정보 조회"""
    logger.info(f"[action] 양식 매칭: '{question}'")

    # 1. 임베딩
    query_vector = get_embedding(question)

    # 2. Pinecone 검색 (결재 카테고리, db_sync layer만)
    matches = search_vectors(
        query_vector,
        company_id,
        top_k=3,
        min_score=0.3,
        category="결재",
        include_platform=False  # 가이드 제외
    )

    if not matches:
        logger.warning(f"[action] 양식 매칭 실패")
        return None

    # 3. 가장 점수 높은 매칭의 document_id 추출
    top_match = matches[0]
    metadata = top_match.metadata

    # filename에서 양식 ID 추출 (예: approval_form_{uuid}.auto.txt)
    document_name_field = metadata.get("document_name", "")
    match = re.search(r"approval_form_([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})", document_name_field)

    if not match:
        logger.warning(f"[action] 파일명에서 documentId 추출 실패: {document_name_field}")
        return None

    document_id = match.group(1)

    logger.info(
        f"[action] 매칭됨: {metadata.get('subcategory')} "
        f"(score={top_match.score:.3f}, document_id={document_id})"
    )

    # 4. approval-service에서 최신 양식 정보 조회 (formSchema 최신화)
    document = await approval_client.get_document_by_id(company_id, document_id)
    if not document:
        logger.error(f"[action] 양식 단건 조회 실패: {document_id}")
        return None

    return document


# ============================================
# Function Calling 슬롯 추출
# ============================================

async def _extract_slots(
        question: str,
        history: str,
        existing_slots: dict,
        form_schema: str,
        document_name: str) -> dict:
    """LLM Function Calling으로 사용자 입력에서 슬롯값 추출"""

    # 빈 양식이면 추출 시도 안 함
    document = {"documentName": document_name, "formSchema": form_schema}
    tool = form_builder.build_tool_from_form(document)
    if not tool:
        return {}


    # _extract_slots 함수 시작 부분에 추가
    today = date.today()
    tomorrow = today + timedelta(days=1)
    day_after = today + timedelta(days=2)
    system_prompt = f"""당신은 HR 결재 양식 작성 어시스턴트입니다.

[현재 날짜 정보]
- 오늘: {today.isoformat()} ({_get_weekday_korean(today)})
- 내일: {tomorrow.isoformat()}
- 모레: {day_after.isoformat()}

[작성 중인 양식]
{document_name}

[이미 입력된 정보]
{json.dumps(existing_slots, ensure_ascii=False) if existing_slots else "(없음)"}

[규칙]
- 사용자 입력에서 양식의 필드값을 추출하세요.
- 추출 가능한 정보가 없으면 함수를 호출하지 마세요.
- 날짜는 반드시 위의 [현재 날짜 정보]를 기준으로 YYYY-MM-DD로 변환하세요.
  - "오늘" → {today.isoformat()}
  - "내일" → {tomorrow.isoformat()}
  - "모레" → {day_after.isoformat()}
  - "다음주 월요일", "이번주 금요일" 등도 [현재 날짜 정보] 기준으로 계산
- 시간은 HH:mm 형식으로 변환하세요.
- 사용자가 명시하지 않은 정보는 추출하지 마세요. (추측 금지)
- title 필드는 양식 내용에 어울리는 자연스러운 제목을 자동 생성해도 됩니다.
- 이미 입력된 정보는 유지하고, 새 정보만 추출하세요.
- 사용일수(usageDays)는 시작일과 종료일이 모두 있으면 자동 계산하세요. (양 끝 포함)
"""

    messages = [
        {"role": "system", "content": system_prompt}
    ]

    if history:
        messages.append({
            "role": "user",
            "content": f"[이전 대화]\n{history}"
        })

    messages.append({
        "role": "user",
        "content": f"[현재 입력]\n{question}"
    })

    try:
        response = openai_client.chat.completions.create(
            model="gpt-4o-mini",
            messages=messages,
            tools=[tool],
            tool_choice="auto",
            temperature=0.0
        )

        message = response.choices[0].message

        # 도구 호출이 없으면 빈 dict
        if not message.tool_calls:
            logger.info(f"[action] 추출된 슬롯 없음")
            return {}

        # 첫 번째 tool_call의 arguments 파싱
        tool_call = message.tool_calls[0]
        arguments = json.loads(tool_call.function.arguments)

        # 빈 값/None 제거
        cleaned = {
            k: v for k, v in arguments.items()
            if v is not None and v != ""
        }

        logger.info(f"[action] 추출된 슬롯: {cleaned}")
        return cleaned

    except Exception as e:
        logger.error(f"[action] 슬롯 추출 실패: {e}")
        return {}


# ============================================
# 결재 제출
# ============================================

async def _submit_request(current_state: ActionState, status: str) -> ActionResponse:
    """결재 생성 (WAIT 또는 DRAFT)"""
    logger.info(f"[action] 결재 생성: status={status}, document={current_state.document_name}")

    # 1. 결재선 후보 조회 (WAIT일 때만)
    approval_lines = []
    if status == "WAIT":
        candidates = await approval_client.get_candidates(
            current_state.company_id,
            current_state.document_id,
            current_state.member_position_id
        )

        approval_lines = _build_approval_lines(candidates)

        if not approval_lines:
            logger.warning(f"[action] 결재선 후보 없음, DRAFT로 변경")
            status = "DRAFT"

    # 2. payload 생성
    payload = {
        "documentId": current_state.document_id,
        "contentJson": json.dumps(current_state.slots, ensure_ascii=False),
        "requestStatus": status,
        "approvalLines": approval_lines,
        "viewers": [],
        "recipients": []
    }

    # 3. 결재 생성 호출
    result = await approval_client.create_request(
        current_state.company_id,
        current_state.member_id,
        current_state.member_position_id,
        payload
    )

    if not result:
        return _error_response(
            "결재 생성에 실패했습니다. 다시 시도해주세요.",
            current_state.session_id
        )

    # 4. 상태 정리
    state.clear_state(current_state.session_id)

    # 5. 응답 분기
    if status == "WAIT":
        return _submitted_response(result, approval_lines)
    else:
        return _draft_response(result)


def _build_approval_lines(candidates: list) -> list:
    """후보 결재자 목록 → 결재선 자동 구성 (각 stepOrder의 첫 번째 후보)"""
    approval_lines = []

    for candidate in candidates:
        step_order = candidate.get("stepOrder")
        candidate_list = candidate.get("candidates", [])

        if not candidate_list:
            logger.warning(f"[action] stepOrder={step_order} 후보 없음")
            continue

        # 첫 번째 후보 자동 선택
        first = candidate_list[0]
        approval_lines.append({
            "approverMemberId": first.get("memberId"),
            "approverMemberPositionId": first.get("memberPositionId"),
            "stepOrder": step_order
        })

    return approval_lines


# ============================================
# 헬퍼: 상태 로드/생성
# ============================================

def _load_or_create_state(
        session_id: Optional[str],
        company_id: str,
        member_id: str,
        member_position_id: str) -> ActionState:
    """기존 세션 로드 or 신규 생성"""
    if session_id:
        existing = state.load_state(session_id)
        if existing:
            return existing

    # 신규 생성
    new_session_id = state.create_session_id()
    return ActionState(
        session_id=new_session_id,
        company_id=company_id,
        member_id=member_id,
        member_position_id=member_position_id,
        slots={}
    )


# ============================================
# 응답 빌더들
# ============================================

def _ask_response(current_state: ActionState, missing: list) -> ActionResponse:
    """추가 정보 요청 응답"""
    first_missing = missing[0]
    label = first_missing["label"]

    # 모든 필요 정보를 한 메시지에 안내
    if len(missing) == 1:
        message = f"{label}을(를) 알려주세요."
    else:
        labels = [m["label"] for m in missing]
        message = f"다음 정보를 알려주세요:\n- " + "\n- ".join(labels)

    # select 타입이면 옵션 안내
    if first_missing.get("type") == "select" and first_missing.get("options"):
        options_str = ", ".join(first_missing["options"])
        message += f"\n\n({label} 옵션: {options_str})"

    return ActionResponse(
        type="ask",
        message=message,
        session_id=current_state.session_id
    )


def _confirm_response(current_state: ActionState) -> ActionResponse:
    """확인 요청 응답"""
    preview_fields = form_builder.render_preview(
        current_state.slots, current_state.form_schema
    )

    preview_text = "\n".join([f"- {k}: {v}" for k, v in preview_fields.items()])
    message = (
        f"다음 내용으로 [{current_state.document_name}]을(를) 작성하시겠습니까?\n\n"
        f"{preview_text}\n\n"
        f"증빙 서류를 첨부하시겠습니까?"
    )

    return ActionResponse(
        type="confirm",
        message=message,
        session_id=current_state.session_id,
        preview={
            "documentName": current_state.document_name,
            "fields": preview_fields
        },
        actions=[
            ActionButton(label="예, 첨부할게요", value="with_attachment"),
            ActionButton(label="아니요, 바로 제출", value="without_attachment"),
            ActionButton(label="취소", value="cancel")
        ]
    )


def _submitted_response(result: dict, approval_lines: list) -> ActionResponse:
    """결재 제출(WAIT) 완료 응답"""
    request_id = result.get("requestId")
    document_number = result.get("documentNumber", "(미발급)")

    approver_count = len(approval_lines)
    message = (
        f"결재가 제출되었습니다.\n"
        f"문서번호: {document_number}\n"
        f"결재자: {approver_count}명"
    )

    return ActionResponse(
        type="submitted",
        message=message,
        request_id=request_id
    )


def _draft_response(result: dict) -> ActionResponse:
    """임시저장(DRAFT) 완료 응답"""
    request_id = result.get("requestId")
    redirect_url = f"/app/approvals?tab=compose&requestId={request_id}"

    message = (
        f"임시저장되었습니다.\n"
        f"결재 작성 화면에서 첨부파일을 추가한 후 제출해주세요.\n\n"
        f"관련 메뉴: {redirect_url}\n"
        f"화면명: 결재 요청 작성"
    )

    return ActionResponse(
        type="draft_for_attachment",
        message=message,
        request_id=request_id,
        redirect_url=redirect_url
    )


def _error_response(message: str, session_id: Optional[str]) -> ActionResponse:
    """에러 응답"""
    return ActionResponse(
        type="error",
        message=message,
        session_id=session_id,
        error_code="GENERIC_ERROR"
    )

def _get_weekday_korean(target_date: date) -> str:
    """date 객체 → 한국어 요일"""
    weekdays = ["월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"]
    return weekdays[target_date.weekday()]