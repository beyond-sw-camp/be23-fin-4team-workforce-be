from pydantic import BaseModel, field_validator
from datetime import date
from typing import Optional, List, Union

class OrganizationInfo(BaseModel):
    organizationId: Optional[str] = None
    name: Optional[str] = None

class MemberSavedEvent(BaseModel):
    memberId: str
    companyId: str
    name: str
    organizationList: Optional[List[OrganizationInfo]] = []
    titleName: Optional[List[str]] = []
    phoneNumber: Optional[str] = None
    email: Optional[str] = None
    memberStatus: Optional[str] = None
    position: Optional[str] = None
    sabun: Optional[str] = None
    joinDate: Optional[Union[date, List[int]]] = None
    jobGradeName: Optional[str] = None
    esgScore: int = 0

    @field_validator("joinDate", mode="before")
    @classmethod
    def parse_join_date(cls, v):
        if v is None:
            return None
        # Java LocalDate 배열 형식 [2025, 4, 1] 처리
        if isinstance(v, list) and len(v) == 3:
            return date(v[0], v[1], v[2])
        return v

class MemberDeletedEvent(BaseModel):
    memberId: str