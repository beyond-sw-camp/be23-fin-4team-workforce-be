"""ai-service /ai/action 요청/응답 스키마"""
from pydantic import BaseModel, Field
from typing import Optional, Any


class ActionRequest(BaseModel):
    """챗봇 액션 요청"""
    question: str = ""
    conversation_history: str = ""
    session_id: Optional[str] = None
    action: Optional[str] = None  # "with_attachment" | "without_attachment" | "cancel"


class PreviewField(BaseModel):
    """미리보기 필드 1개"""
    label: str
    value: Any


class ActionButton(BaseModel):
    """사용자에게 표시할 액션 버튼"""
    label: str
    value: str  # action 값 (with_attachment 등)


class ActionResponse(BaseModel):
    """챗봇 액션 응답"""
    type: str  # "ask" | "confirm" | "submitted" | "draft_for_attachment" | "error" | "cancelled"
    message: str
    session_id: Optional[str] = None

    # type="confirm"일 때
    preview: Optional[dict] = None
    actions: Optional[list[ActionButton]] = None

    # type="submitted" 또는 "draft_for_attachment"일 때
    request_id: Optional[str] = None
    redirect_url: Optional[str] = None

    # type="error"일 때
    error_code: Optional[str] = None


class ActionState(BaseModel):
    """Redis에 저장되는 슬롯 상태"""
    session_id: str
    company_id: str
    member_id: str
    member_position_id: str

    # 양식 정보
    document_id: Optional[str] = None
    document_name: Optional[str] = None
    form_schema: Optional[str] = None
    request_type: Optional[str] = None  # VACATION, ATTENDANCE 등

    # 채워진 슬롯
    slots: dict = Field(default_factory=dict)