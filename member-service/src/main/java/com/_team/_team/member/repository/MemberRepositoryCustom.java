package com._team._team.member.repository;

import com._team._team.member.dto.reqdto.MemberSearchCondition;
import com._team._team.member.dto.resdto.MemberSearchItemResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// QueryDSL 기반 사원 검색 인터페이스
public interface MemberRepositoryCustom {

    // 동적 조건 + 페이징으로 검색
    // 활성 포지션 1건만 조인 (없으면 null)
    Page<MemberSearchItemResDto> searchMembers(MemberSearchCondition condition, Pageable pageable);
}
