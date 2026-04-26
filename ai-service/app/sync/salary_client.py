"""salary-service API 호출 클라이언트"""
import httpx
import logging

logger = logging.getLogger(__name__)

GATEWAY_URL = "http://localhost:8080"


# === 기존 함수들 ===
async def get_company_leave_types(company_id: str) -> list[dict]:
    url = f"{GATEWAY_URL}/attendance/leave-types/internal"
    return await _internal_get(url, company_id, "휴가 종류")


async def get_leave_policies(company_id: str) -> list[dict]:
    url = f"{GATEWAY_URL}/leave-policies/internal"
    return await _internal_get(url, company_id, "연차 정책")


async def get_work_schedules(company_id: str) -> list[dict]:
    url = f"{GATEWAY_URL}/work-schedules/internal"
    return await _internal_get(url, company_id, "근무 스케줄")


async def get_overtime_policies(company_id: str) -> list[dict]:
    url = f"{GATEWAY_URL}/attendance/overtime-policies/internal"
    return await _internal_get(url, company_id, "연장근로 정책")


async def get_company_holidays(company_id: str) -> list[dict]:
    url = f"{GATEWAY_URL}/company-holidays/internal"
    return await _internal_get(url, company_id, "공휴일")


# === 신규 함수 (Phase 2-B) ===
async def get_salary_item_templates(company_id: str) -> list[dict]:
    """GET /salary/salary-item-templates/internal?companyId=xxx"""
    url = f"{GATEWAY_URL}/salary/salary-item-templates/internal"
    return await _internal_get(url, company_id, "급여 항목 템플릿")


async def get_salary_policies(company_id: str) -> list[dict]:
    """GET /salary/salary-policies/internal?companyId=xxx"""
    url = f"{GATEWAY_URL}/salary/salary-policies/internal"
    return await _internal_get(url, company_id, "급여 정책")


async def get_pay_grade_tables(company_id: str) -> list[dict]:
    """GET /salary/pay-grade-table/internal?companyId=xxx"""
    url = f"{GATEWAY_URL}/salary/pay-grade-table/internal"
    return await _internal_get(url, company_id, "호봉표")


# === 공통 헬퍼 ===
async def _internal_get(url: str, company_id: str, label: str) -> list[dict]:
    params = {"companyId": company_id}

    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get(url, params=params)
        response.raise_for_status()
        result = response.json()
        data = result.get("data", [])

        logger.info(
            f"[salary_client] {label} {len(data)}개 조회 완료 "
            f"(company={company_id})"
        )
        return data