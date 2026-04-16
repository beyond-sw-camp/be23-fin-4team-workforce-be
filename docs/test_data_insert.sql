-- =====================================================================
-- 테스트 데이터 생성 스크립트 (MySQL/MariaDB)
-- createTestData() 의 Java 로직과 동일한 결과를 생성합니다.
--
-- 주의:
-- 1) password 컬럼은 BCrypt 인코딩된 값이어야 합니다.
--    아래 @ADMIN_PW, @MEMBER_PW 에 BCryptPasswordEncoder 로 생성한
--    실제 해시값을 넣어야 로그인이 정상 동작합니다.
--      - admin*@workforce.com / test1234!
--      - emp{c}_{i}@gmail.com / Test1234!
--    (예시 해시는 placeholder 입니다. 실 환경에 맞게 교체하세요.)
-- 2) Permission / RolePermission 은 온보딩 로직이 자동 세팅하는
--    부분이라 SQL 로 재현하려면 너무 방대해서 이 스크립트는
--    Role 까지만 생성합니다. 권한 매핑이 필요하면 별도로 seeding 필요.
-- 3) 스크립트는 3회 반복(회사1~3)으로 동작합니다.
-- =====================================================================

SET @ADMIN_PW  = '$2a$10$REPLACE_WITH_BCRYPT_HASH_FOR_test1234!';
SET @MEMBER_PW = '$2a$10$REPLACE_WITH_BCRYPT_HASH_FOR_Test1234!';

DROP PROCEDURE IF EXISTS seed_test_data;
DELIMITER $$

CREATE PROCEDURE seed_test_data()
BEGIN
    DECLARE c INT DEFAULT 1;

    WHILE c <= 3 DO

        -- ========== 1) Company ==========
        SET @company_id      = UUID();
        SET @company_name    = CONCAT('워크포스 컴퍼니 ', c);
        SET @company_domain  = CONCAT('workforce', c);

        INSERT INTO company (
            company_id, company_name, company_domain, ceo_name,
            business_number, logo_url, address, detail_address,
            status, created_at, updated_at
        ) VALUES (
            @company_id, @company_name, @company_domain, CONCAT('김대표', c),
            CONCAT('123456789', c), NULL,
            CONCAT('서울시 강남구 테헤란로 ', c), CONCAT(c, '층'),
            'ACTIVE', NOW(), NOW()
        );

        -- ========== 2) 기본 조직(최상위) ==========
        SET @top_org_id = UUID();
        INSERT INTO organization (
            organization_id, company_id, parent_id, name,
            display_order, del_yn, created_at, updated_at
        ) VALUES (
            @top_org_id, @company_id, NULL, @company_name,
            0, 'NO', NOW(), NOW()
        );

        -- ========== 3) 온보딩 기본 직급/직책 ("관리자") ==========
        SET @admin_grade_id = UUID();
        INSERT INTO job_grade (
            job_grade_id, company_id, name, display_order,
            del_yn, created_at, updated_at
        ) VALUES (
            @admin_grade_id, @company_id, '관리자', 0, 'NO', NOW(), NOW()
        );

        SET @admin_title_id = UUID();
        INSERT INTO job_title (
            job_title_id, company_id, name, display_order,
            del_yn, created_at, updated_at
        ) VALUES (
            @admin_title_id, @company_id, '관리자', 0, 'NO', NOW(), NOW()
        );

        -- ========== 4) 기본 4개 역할 ==========
        SET @role_sys_admin_id = UUID();
        SET @role_hr_admin_id  = UUID();
        SET @role_leader_id    = UUID();
        SET @role_employee_id  = UUID();

        INSERT INTO role (role_id, company_id, name, description, display_order, del_yn, created_at, updated_at) VALUES
            (@role_sys_admin_id, @company_id, '시스템 관리자', NULL, 1, 'NO', NOW(), NOW()),
            (@role_hr_admin_id,  @company_id, '인사 관리자',   NULL, 2, 'NO', NOW(), NOW()),
            (@role_leader_id,    @company_id, '팀장',          NULL, 3, 'NO', NOW(), NOW()),
            (@role_employee_id,  @company_id, '일반 직원',     NULL, 4, 'NO', NOW(), NOW());

        -- ========== 5) 관리자 Member + MemberPosition ==========
        SET @admin_id    = UUID();
        SET @admin_email = CONCAT('admin', c, '@workforce.com');

        INSERT INTO member (
            member_id, company_id, default_position_id, email, personal_email,
            password, name, phone_public_yn, address_public_yn,
            member_status, account_status, employment_type,
            login_fail_count, is_first_login_yn, is_email_verified_yn, del_yn,
            esg_score, join_date, created_at, updated_at
        ) VALUES (
            @admin_id, @company_id, NULL, @admin_email, NULL,
            @ADMIN_PW, CONCAT('관리자', c), 'NO', 'NO',
            'ACTIVE', 'ACTIVE', 'FULL_TIME',
            0, 'NO', 'YES', 'NO',
            0, CURDATE(), NOW(), NOW()
        );

        SET @admin_position_id = UUID();
        INSERT INTO member_position (
            member_position_id, member_id, organization_id, job_title_id,
            job_grade_id, role_id, start_date, end_date,
            is_active_yn, is_system_admin_yn, del_yn,
            created_at, updated_at
        ) VALUES (
            @admin_position_id, @admin_id, @top_org_id, @admin_title_id,
            @admin_grade_id, @role_sys_admin_id, CURDATE(), NULL,
            'YES', 'YES', 'NO', NOW(), NOW()
        );

        UPDATE member SET default_position_id = @admin_position_id
         WHERE member_id = @admin_id;

        -- ========== 6) 하위 조직 (인사팀, 개발팀) ==========
        SET @hr_team_id = UUID();
        INSERT INTO organization (
            organization_id, company_id, parent_id, name,
            display_order, del_yn, created_at, updated_at
        ) VALUES (
            @hr_team_id, @company_id, @top_org_id, '인사팀',
            1, 'NO', NOW(), NOW()
        );

        SET @dev_team_id = UUID();
        INSERT INTO organization (
            organization_id, company_id, parent_id, name,
            display_order, del_yn, created_at, updated_at
        ) VALUES (
            @dev_team_id, @company_id, @top_org_id, '개발팀',
            2, 'NO', NOW(), NOW()
        );

        -- ========== 7) 추가 직급 (과장, 사원) ==========
        SET @grade_senior_id = UUID();
        SET @grade_junior_id = UUID();
        INSERT INTO job_grade (job_grade_id, company_id, name, display_order, del_yn, created_at, updated_at) VALUES
            (@grade_senior_id, @company_id, '과장', 1, 'NO', NOW(), NOW()),
            (@grade_junior_id, @company_id, '사원', 2, 'NO', NOW(), NOW());

        -- ========== 8) 추가 직책 (팀장, 팀원) ==========
        SET @title_leader_id = UUID();
        SET @title_member_id = UUID();
        INSERT INTO job_title (job_title_id, company_id, name, display_order, del_yn, created_at, updated_at) VALUES
            (@title_leader_id, @company_id, '팀장', 1, 'NO', NOW(), NOW()),
            (@title_member_id, @company_id, '팀원', 2, 'NO', NOW(), NOW());

        -- ========== 9) 직원 10명 생성 ==========
        -- i=0~9 규칙:
        --   team  : i<5 → 인사팀, else 개발팀
        --   grade : i<3 → 과장, else 사원
        --   title : i==0 or i==5 → 팀장, else 팀원
        --   role  : i==0 or i==5 → 팀장 역할, else 일반 직원

        SET @i = 0;
        WHILE @i < 10 DO

            -- 이름 / 이니셜
            SET @name = ELT(@i + 1,
                '박세민','이지연','이다은','김정훈','김도균',
                '황주안','이병찬','홍진희','박준형','정명진');
            SET @initial = ELT(@i + 1,
                'KIS','PIS','LGH','JMK','CYB',
                'HBE','MFE','SDS','HDT','OIF');

            -- 입사일
            SET @join_date = CASE @i
                WHEN 0 THEN '2025-01-02'
                WHEN 1 THEN '2025-02-03'
                WHEN 2 THEN '2025-03-04'
                WHEN 3 THEN DATE_SUB(CURDATE(), INTERVAL 3 YEAR)
                WHEN 4 THEN DATE_SUB(CURDATE(), INTERVAL 5 YEAR)
                WHEN 5 THEN DATE_SUB(CURDATE(), INTERVAL 2 YEAR)
                WHEN 6 THEN DATE_SUB(CURDATE(), INTERVAL 20 MONTH)
                WHEN 7 THEN '2025-04-01'
                WHEN 8 THEN '2025-05-01'
                WHEN 9 THEN '2025-06-01'
            END;

            -- 고용형태
            SET @emp_type = CASE @i
                WHEN 2 THEN 'CONTRACT'
                WHEN 6 THEN 'INTERN'
                WHEN 8 THEN 'CONTRACT'
                ELSE 'FULL_TIME'
            END;

            -- 팀/직급/직책/역할 분기
            SET @team_id  = IF(@i < 5, @hr_team_id, @dev_team_id);
            SET @grade_id = IF(@i < 3, @grade_senior_id, @grade_junior_id);
            SET @is_lead  = (@i = 0 OR @i = 5);
            SET @title_id = IF(@is_lead, @title_leader_id, @title_member_id);
            SET @role_id  = IF(@is_lead, @role_leader_id, @role_employee_id);

            -- 사번 / 회사이메일 (createMember 규칙: {sabun}.{initial}@{domain}.company)
            SET @sabun     = LPAD(@i + 1, 4, '0');
            SET @comp_mail = CONCAT(@sabun, '.', @initial, '@', @company_domain, '.company');
            SET @pers_mail = CONCAT('emp', c, '_', @i + 1, '@gmail.com');

            -- Member
            SET @mem_id = UUID();
            INSERT INTO member (
                member_id, company_id, default_position_id, email, personal_email,
                password, name, sabun, phone_public_yn, address_public_yn,
                member_status, account_status, employment_type,
                login_fail_count, is_first_login_yn, is_email_verified_yn, del_yn,
                esg_score, join_date, created_at, updated_at
            ) VALUES (
                @mem_id, @company_id, NULL, @comp_mail, @pers_mail,
                @MEMBER_PW, CONCAT(@name, c), @sabun, 'NO', 'NO',
                'ACTIVE', 'ACTIVE', @emp_type,
                0, 'NO', 'NO', 'NO',
                0, @join_date, NOW(), NOW()
            );

            -- MemberPosition
            SET @pos_id = UUID();
            INSERT INTO member_position (
                member_position_id, member_id, organization_id, job_title_id,
                job_grade_id, role_id, start_date, end_date,
                is_active_yn, is_system_admin_yn, del_yn,
                created_at, updated_at
            ) VALUES (
                @pos_id, @mem_id, @team_id, @title_id,
                @grade_id, @role_id, @join_date, NULL,
                'YES', 'NO', 'NO', NOW(), NOW()
            );

            UPDATE member SET default_position_id = @pos_id WHERE member_id = @mem_id;

            SET @i = @i + 1;
        END WHILE;

        SET c = c + 1;
    END WHILE;
END $$
DELIMITER ;

-- 실행
CALL seed_test_data();

-- 필요 시 정리
-- DROP PROCEDURE IF EXISTS seed_test_data;
