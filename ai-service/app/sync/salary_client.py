"""salary-service API 호출 클라이언트"""
import httpx
import logging

logger = logging.getLogger(__name__)

GATEWAY_URL = "http://localhost:8080"

# 시스템 호출용 더미 UUID
SYSTEM_USER_UUID = "00000000-0000-0000-0000-000000000000"


# === 기존 함수 ===
async def get_company_leave_types(company_id: str) -> list[dict]:
    url = f"{GATEWAY_URL}/attendance/leave-types/internal"
    return await _internal_get(url, company_id, "휴가 종류")


async def get_leave_policies(company_id: str) -> list[dict]:
    url = f"{GATEWAY_URL}/leave-policies/internal"
    return await _internal_get(url, company_id, "연차 정책")


# === 신규 함수 (Phase 2-C) ===
async def get_work_schedules(company_id: str) -> list[dict]:
    """GET /work-schedules/internal?companyId=xxx"""
    url = f"{GATEWAY_URL}/work-schedules/internal"
    return await _internal_get(url, company_id, "근무 스케줄")


async def get_overtime_policies(company_id: str) -> list[dict]:
    """GET /attendance/overtime-policies/internal?companyId=xxx"""
    url = f"{GATEWAY_URL}/attendance/overtime-policies/internal"
    return await _internal_get(url, company_id, "연장근로 정책")


async def get_company_holidays(company_id: str) -> list[dict]:
    """GET /company-holidays/internal?companyId=xxx"""
    url = f"{GATEWAY_URL}/company-holidays/internal"
    return await _internal_get(url, company_id, "공휴일")


# === 공통 헬퍼 ===
async def _internal_get(url: str, company_id: str, label: str) -> list[dict]:
    """내부 API 공통 호출"""
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