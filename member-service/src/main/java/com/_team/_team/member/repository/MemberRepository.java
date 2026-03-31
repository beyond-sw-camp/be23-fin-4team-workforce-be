package com._team._team.member.repository;

import com._team._team.company.domain.Company;
import com._team._team.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 이메일로 회원 조회 (로그인)
    Optional<Member> findByEmail(String email);

    // 개인 이메일 중복 체크
    boolean existsByPersonalEmail(String personalEmail);

    // 사번 + 회사 중복 체크
    boolean existsBySabunAndCompany(String sabun, Company company);

    // 회사별 직원 목록
    List<Member> findByCompanyAndDelYn(Company company, String delYn);

    Optional<Member> findByPersonalEmail(String personalEmail);
}
