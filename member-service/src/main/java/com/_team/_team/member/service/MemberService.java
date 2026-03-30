package com._team._team.member.service;

import com._team._team.company.domain.Company;
import com._team._team.company.dto.reqdto.CompanyOnboardingReqDto;
import com._team._team.company.service.MailService;
import com._team._team.dto.BusinessException;
import com._team._team.member.auth.JwtTokenProvider;
import com._team._team.annotation.Action;
import com._team._team.member.constant.PermissionRange;
import com._team._team.annotation.Resource;
import com._team._team.member.domain.*;
import com._team._team.member.domain.enums.AccountStatus;
import com._team._team.member.domain.enums.EmploymentType;
import com._team._team.member.domain.enums.MemberStatus;
import com._team._team.member.dto.reqdto.ChangePasswordReqDto;
import com._team._team.member.dto.reqdto.LoginReqDto;
import com._team._team.member.dto.reqdto.MemberCreateReqDto;
import com._team._team.member.dto.resdto.LoginResDto;
import com._team._team.member.dto.resdto.MemberResDto;
import com._team._team.member.repository.*;
import com._team._team.organization.domain.JobGrade;
import com._team._team.organization.domain.JobTitle;
import com._team._team.organization.domain.Organization;
import com._team._team.organization.repository.JobGradeRepository;
import com._team._team.organization.repository.JobTitleRepository;
import com._team._team.organization.repository.OrganizationRepository;
import com._team._team.util.PermissionUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final JobGradeRepository jobGradeRepository;
    private final JobTitleRepository jobTitleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final RedisTemplate<String, String> permissoinRedisTemplate;
    private final PermissionUtils permissionUtils;

    @Autowired
    public MemberService(MemberRepository memberRepository, MemberPositionRepository memberPositionRepository, RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository, PermissionRepository permissionRepository, OrganizationRepository organizationRepository, JobGradeRepository jobGradeRepository, JobTitleRepository jobTitleRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, MailService mailService, @Qualifier("permissionInventory") RedisTemplate<String, String> permissoinRedisTemplate, PermissionUtils permissionUtils) {
        this.memberRepository = memberRepository;
        this.memberPositionRepository = memberPositionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.organizationRepository = organizationRepository;
        this.jobGradeRepository = jobGradeRepository;
        this.jobTitleRepository = jobTitleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.mailService = mailService;
        this.permissoinRedisTemplate = permissoinRedisTemplate;
        this.permissionUtils = permissionUtils;
    }

    // 온보딩 시 기본 역할 4개 + 권한 생성
    public void createDefaultRoles(Company company) {

        // 1. 기본 역할 4개 생성
        Role systemAdmin = createRole(company, "시스템 관리자", 1);
        Role hrManager = createRole(company, "인사 관리자", 2);
        Role teamLeader = createRole(company, "팀장", 3);
        Role employee = createRole(company, "일반 직원", 4);

        // 2. 기본 권한 세팅
        List<RolePermission> rolePermissions = new ArrayList<>();

        // 인사 관리자 권한
        rolePermissions.addAll(buildRolePermissions(hrManager, Resource.MEMBER,
                List.of(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE), PermissionRange.COMPANY));
        rolePermissions.addAll(buildRolePermissions(hrManager, Resource.ORGANIZATION,
                List.of(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE), PermissionRange.COMPANY));
        rolePermissions.addAll(buildRolePermissions(hrManager, Resource.SALARY,
                List.of(Action.READ), PermissionRange.COMPANY));
        rolePermissions.addAll(buildRolePermissions(hrManager, Resource.ATTENDANCE,
                List.of(Action.READ, Action.UPDATE), PermissionRange.COMPANY));
        rolePermissions.addAll(buildRolePermissions(hrManager, Resource.APPROVAL,
                List.of(Action.READ), PermissionRange.COMPANY));
        rolePermissions.addAll(buildRolePermissions(hrManager, Resource.ROLE,
                List.of(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE), PermissionRange.COMPANY));
        rolePermissions.addAll(buildRolePermissions(hrManager, Resource.GOAL,
                List.of(Action.READ), PermissionRange.COMPANY));
        rolePermissions.addAll(buildRolePermissions(hrManager, Resource.EVALUATION,
                List.of(Action.READ), PermissionRange.COMPANY));

        // 팀장 권한
        rolePermissions.addAll(buildRolePermissions(teamLeader, Resource.MEMBER,
                List.of(Action.READ), PermissionRange.TEAM));
        rolePermissions.addAll(buildRolePermissions(teamLeader, Resource.ATTENDANCE,
                List.of(Action.READ, Action.UPDATE), PermissionRange.TEAM));
        rolePermissions.addAll(buildRolePermissions(teamLeader, Resource.SALARY,
                List.of(Action.READ), PermissionRange.TEAM));
        rolePermissions.addAll(buildRolePermissions(teamLeader, Resource.APPROVAL,
                List.of(Action.READ, Action.UPDATE), PermissionRange.TEAM));
        rolePermissions.addAll(buildRolePermissions(teamLeader, Resource.GOAL,
                List.of(Action.CREATE, Action.READ, Action.UPDATE), PermissionRange.TEAM));
        rolePermissions.addAll(buildRolePermissions(teamLeader, Resource.EVALUATION,
                List.of(Action.CREATE, Action.READ), PermissionRange.TEAM));

        // 일반 직원 권한
        rolePermissions.addAll(buildRolePermissions(employee, Resource.MEMBER,
                List.of(Action.READ), PermissionRange.SELF));
        rolePermissions.addAll(buildRolePermissions(employee, Resource.ATTENDANCE,
                List.of(Action.READ, Action.CREATE), PermissionRange.SELF));
        rolePermissions.addAll(buildRolePermissions(employee, Resource.SALARY,
                List.of(Action.READ), PermissionRange.SELF));
        rolePermissions.addAll(buildRolePermissions(employee, Resource.APPROVAL,
                List.of(Action.READ, Action.CREATE), PermissionRange.SELF));
        rolePermissions.addAll(buildRolePermissions(employee, Resource.GOAL,
                List.of(Action.CREATE, Action.READ), PermissionRange.SELF));
        rolePermissions.addAll(buildRolePermissions(employee, Resource.EVALUATION,
                List.of(Action.READ), PermissionRange.SELF));

        rolePermissionRepository.saveAll(rolePermissions);
    }

    public void createAdminMember(
            CompanyOnboardingReqDto reqDto,
            Company company) {

        // 이메일 중복 체크
        if (memberRepository.existsByEmail(reqDto.getAdminEmail())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."
            );
        }

        // 관리자 계정 생성
        Member admin = Member.builder()
                .company(company)
                .email(reqDto.getAdminEmail())
                .password(passwordEncoder.encode(reqDto.getAdminPassword())) // 본인 비밀번호
                .name(reqDto.getAdminName())
                .memberStatus(MemberStatus.ACTIVE)
                .accountStatus(AccountStatus.ACTIVE)
                .employmentType(EmploymentType.FULL_TIME)
                .joinDate(LocalDate.now())
                .isEmailVerifiedYn("YES")  // 이미 인증 완료
                .isFirstLoginYn("NO")      // 비밀번호 직접 설정
                .build();

        Member savedAdmin = memberRepository.save(admin);

        // 기본 조직/직급/직책/역할 조회
        Organization defaultOrg = organizationRepository
                .findByCompany_CompanyIdAndDelYn(company.getCompanyId(), "NO")
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "기본 조직을 찾을 수 없습니다."
                ));

        JobGrade defaultGrade = jobGradeRepository
                .findByCompany_CompanyIdAndDelYnOrderByDisplayOrder(company.getCompanyId(), "NO")
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "기본 직급을 찾을 수 없습니다."
                ));

        JobTitle defaultTitle = jobTitleRepository
                .findByCompany_CompanyIdAndDelYnOrderByDisplayOrder(company.getCompanyId(), "NO")
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "기본 직책을 찾을 수 없습니다."
                ));

        Role systemAdminRole = roleRepository
                .findByCompany_CompanyIdAndDelYnOrderByDisplayOrder(company.getCompanyId(), "NO")
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "기본 역할을 찾을 수 없습니다."
                ));

        // 관리자 MemberPosition 생성
        MemberPosition position = MemberPosition.builder()
                .member(savedAdmin)
                .organization(defaultOrg)
                .jobGrade(defaultGrade)
                .jobTitle(defaultTitle)
                .role(systemAdminRole)
                .startDate(LocalDate.now())
                .isSystemAdminYn("YES")
                .build();

        MemberPosition savedPosition = memberPositionRepository.save(position);

        // defaultPositionId 업데이트
        savedAdmin.updateDefaultPosition(savedPosition.getMemberPositionId());
    }

    // 역할 생성 헬퍼
    private Role createRole(Company company, String name, int displayOrder) {
        Role role = Role.builder()
                .company(company)
                .name(name)
                .displayOrder(displayOrder)
                .build();
        return roleRepository.save(role);
    }

    // RolePermission 생성 헬퍼
    private List<RolePermission> buildRolePermissions(
            Role role,
            Resource resource,
            List<Action> actions,
            PermissionRange range) {

        List<RolePermission> result = new ArrayList<>();

        for (Action action : actions) {
            Permission permission = permissionRepository
                    .findByResourceAndAction(resource, action)
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder()
                                    .resource(resource)
                                    .action(action)
                                    .build()
                    ));

            result.add(RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .permissionRange(range)
                    .build());
        }
        return result;
    }
    //이메일 검증
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }
    // 권한 캐싱 메서드 추가
    private void cachePermissions(MemberPosition memberPosition) {

        if ("YES".equals(memberPosition.getIsSystemAdminYn())) return;

        List<String> permissions = memberPosition.getRole()
                .getRolePermissionList()
                .stream()
                .map(rp -> rp.getPermission().getResource().name()
                        + ":" + rp.getPermission().getAction().name()
                        + ":" + rp.getPermissionRange().name())
                .collect(Collectors.toList());

        if (permissions.isEmpty()) {
            log.warn("권한 목록이 비어있음: {}", memberPosition.getMemberPositionId());
            return;
        }

        // Redis에 저장 (30분)
        // key: "PERMISSION:{memberPositionId}"
        // value: "MEMBER:READ:COMPANY,MEMBER:CREATE:COMPANY,..."
        permissoinRedisTemplate.opsForValue().set(
                "PERMISSION:" + memberPosition.getMemberPositionId(),
                String.join(",", permissions),
                30,
                TimeUnit.MINUTES
        );
    }

    // 로그인
    public LoginResDto login(LoginReqDto reqDto, HttpServletResponse response) {

        // 1. 이메일 확인
        Member member = memberRepository.findByEmail(reqDto.getEmail())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."
                ));

        // 2. 계정 잠금 확인
        if (member.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "계정이 잠금 상태입니다. 관리자에게 문의하세요."
            );
        }

        // 3. 비밀번호 확인
        if (!passwordEncoder.matches(reqDto.getPassword(), member.getPassword())) {
            member.increaseLoginFailCount();
            memberRepository.save(member);
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }

        // 4. 로그인 실패 횟수 초기화
        member.resetLoginFailCount();
        memberRepository.save(member);

        // 5. defaultPosition 조회
        MemberPosition memberPosition = memberPositionRepository
                .findByIdWithRoleAndPermissions(member.getDefaultPositionId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "포지션 정보를 찾을 수 없습니다."
                ));

        // 6. AT 발급
        String accessToken = jwtTokenProvider.createAtToken(member, memberPosition);

        // 7. RT 발급 + 쿠키 저장
        String refreshToken = jwtTokenProvider.createRtToken(member);
        jwtTokenProvider.setRtCookie(response, refreshToken);

    cachePermissions(memberPosition);

        return LoginResDto.builder()
                .accessToken(accessToken)
                .name(member.getName())
                .memberId(member.getMemberId())
                .memberPositionId(memberPosition.getMemberPositionId())
                .isEmailVerifiedYn(member.getIsEmailVerifiedYn())
                .isFirstLoginYn(member.getIsFirstLoginYn())
                .build();
    }

    public void changePassword(UUID memberId, ChangePasswordReqDto reqDto) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));

        // 1. 현재 비밀번호 확인
        if (!passwordEncoder.matches(reqDto.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."
            );
        }

        // 2. 새 비밀번호 확인
        if (!reqDto.getNewPassword().equals(reqDto.getNewPasswordCheck())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "새 비밀번호가 일치하지 않습니다."
            );
        }

        // 3. 현재 비밀번호와 새 비밀번호 같은지 확인
        if (passwordEncoder.matches(reqDto.getNewPassword(), member.getPassword())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "현재 비밀번호와 다른 비밀번호를 입력해주세요."
            );
        }

        // 4. 비밀번호 변경
        member.updatePassword(passwordEncoder.encode(reqDto.getNewPassword()));

        // 5. 최초 로그인 완료 처리
        member.completeFirstLogin();
    }

    // 로그아웃
    public void logout(UUID memberId, HttpServletResponse response) {
        // Redis RT 삭제
        jwtTokenProvider.deleteRtToken(memberId.toString());
        // 쿠키 삭제
        jwtTokenProvider.deleteRtCookie(response);
    }

    // AT 재발급
    public LoginResDto generateAt(UUID memberId, UUID memberPositionId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));

        MemberPosition memberPosition = memberPositionRepository
                .findById(memberPositionId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 포지션입니다."
                ));

        String accessToken = jwtTokenProvider.createAtToken(member, memberPosition);

        return LoginResDto.builder()
                .accessToken(accessToken)
                .memberId(memberId)
                .memberPositionId(memberPositionId)
                .build();
    }
    // 직원 계정 생성
    public void createMember(UUID memberId, UUID memberPositionId, MemberCreateReqDto reqDto) {

        // 1. 요청자 조회
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));
        Company company = requester.getCompany();

        // 2. 회사 이메일 자동 생성
        String companyDomain = company.getCompanyDomain();

        String companyEmail = reqDto.getSabun().toLowerCase()
                + "." + reqDto.getEnglishInitial().toUpperCase()
                + "@" + companyDomain + ".company";

        // 3. 중복 체크
        if (memberRepository.existsByEmail(companyEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용중인 회사 이메일입니다.");
        }
        if (memberRepository.existsByPersonalEmail(reqDto.getPersonalEmail())) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용중인 개인 이메일입니다.");
        }
        if (memberRepository.existsBySabunAndCompany(reqDto.getSabun(), company)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용중인 사번입니다.");
        }

        // 4. 임시 비밀번호 생성
        String tempPassword = generateTempPassword();

        // 5. 직원 계정 생성
        Member member = reqDto.toEntity(companyEmail, passwordEncoder.encode(tempPassword), company);
        Member savedMember = memberRepository.save(member);

        // 6. 조직 조회 + 같은 회사 검증
        Organization organization = organizationRepository
                .findById(reqDto.getOrganizationId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 조직입니다."));

        if (!organization.getCompany().getCompanyId().equals(company.getCompanyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 조직에 직원을 추가할 수 없습니다.");
        }

        // 7. 직급 조회 + 같은 회사 검증
        JobGrade jobGrade = jobGradeRepository
                .findById(reqDto.getJobGradeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 직급입니다."));

        if (!jobGrade.getCompany().getCompanyId().equals(company.getCompanyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 직급을 지정할 수 없습니다.");
        }

        // 8. 직책 조회 + 같은 회사 검증
        JobTitle jobTitle = jobTitleRepository
                .findById(reqDto.getJobTitleId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 직책입니다."));

        if (!jobTitle.getCompany().getCompanyId().equals(company.getCompanyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 직책을 지정할 수 없습니다.");
        }

        // 9. 역할 조회 + 같은 회사 검증
        Role role = roleRepository
                .findById(reqDto.getRoleId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 역할입니다."));

        if (!role.getCompany().getCompanyId().equals(company.getCompanyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 역할을 지정할 수 없습니다.");
        }

        // 10. MemberPosition 생성
        MemberPosition position = MemberPosition.builder()
                .member(savedMember)
                .organization(organization)
                .jobGrade(jobGrade)
                .jobTitle(jobTitle)
                .role(role)
                .startDate(reqDto.getJoinDate())
                .isSystemAdminYn("NO")
                .build();

        MemberPosition savedPosition = memberPositionRepository.save(position);

        // 11. defaultPositionId 업데이트
        savedMember.updateDefaultPosition(savedPosition.getMemberPositionId());

        // 12. 임시 비밀번호 개인 이메일로 발송
        mailService.sendTempPassword(reqDto.getPersonalEmail(), companyEmail, tempPassword);
    }

    // 직원 목록 조회
    @Transactional(readOnly = true)
    public List<MemberResDto> getMemberList(UUID memberId, UUID memberPositionId) {

        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));

        Organization organization = memberPositionRepository
                .findById(memberPositionId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 포지션입니다."
                ))
                .getOrganization();

        return permissionUtils.getDataByRange(
                memberPositionId.toString(),
                Resource.MEMBER,
                Action.READ,

                // COMPANY → 전사 조회
                () -> memberRepository
                        .findByCompanyAndDelYn(requester.getCompany(), "NO")
                        .stream().map(MemberResDto::fromEntity).toList(),

                // TEAM/DEPARTMENT → 같은 조직 조회
                () -> memberPositionRepository
                        .findByOrganizationAndDelYn(organization, "NO")
                        .stream()
                        .map(mp -> MemberResDto.fromEntity(mp.getMember()))
                        .toList(),

                // SELF → 본인만
                () -> List.of(MemberResDto.fromEntity(requester))
        );
    }

    // 직원 상세 조회
    @Transactional(readOnly = true)
    public MemberResDto getMemberDetail(UUID memberId, UUID memberPositionId, UUID targetMemberId) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 직원입니다."
                ));
        return MemberResDto.fromEntity(member);
    }

    // 임시 비밀번호 생성
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

