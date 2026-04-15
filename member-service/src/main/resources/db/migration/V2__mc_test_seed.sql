-- ================================================================
-- member-chat 테스트용 시드 스크립트
-- ================================================================
-- 대상 DB: MariaDB / MySQL (BINARY(16) UUID 저장)
-- 실행 방법: mysql -u <user> -p <db> < V2__mc_test_seed.sql
--
-- 이 스크립트는 member/company 테이블에 이미 레코드가 있다고 가정하지 않는다.
-- 실제 회원 데이터와 무관하게 채팅 도메인만으로 동작을 검증하기 위해
-- 채팅 서비스는 BINARY(16) UUID 를 그대로 저장하므로, 여기서는 고정 UUID 상수를 사용한다.
-- (ChatRoom / ChatMessage 가 실제 member 테이블에 FK 를 걸지 않기 때문에 독립 실행 가능)
-- ================================================================

-- 기존 테스트 데이터 정리 (필요 시 주석 해제)
-- DELETE FROM mc_chat_audit_log;
-- DELETE FROM mc_chat_read_receipt;
-- DELETE FROM mc_chat_message_revision;
-- DELETE FROM mc_chat_message_image;
-- DELETE FROM mc_chat_message;
-- DELETE FROM mc_chat_participant;
-- DELETE FROM mc_chat_legal_hold;
-- DELETE FROM mc_chat_room;

-- ----------------------------------------------------------------
-- 0. 식별자 세팅 (BINARY(16) 로 통일)
-- ----------------------------------------------------------------
SET @company    := UNHEX(REPLACE('11111111-1111-1111-1111-111111111111','-',''));
SET @admin      := UNHEX(REPLACE('aaaaaaaa-0000-0000-0000-000000000001','-',''));
SET @alice      := UNHEX(REPLACE('aaaaaaaa-0000-0000-0000-000000000002','-',''));
SET @bob        := UNHEX(REPLACE('aaaaaaaa-0000-0000-0000-000000000003','-',''));
SET @carol      := UNHEX(REPLACE('aaaaaaaa-0000-0000-0000-000000000004','-',''));

-- ----------------------------------------------------------------
-- 1. 방 생성 : GROUP 1개 + DIRECT 1개 + ANNOUNCEMENT 1개
-- ----------------------------------------------------------------
INSERT INTO mc_chat_room(name, type, company_id, created_by, deleted, legal_hold, created_at, updated_at)
VALUES ('HR 공지방',          'ANNOUNCEMENT', @company, @admin, FALSE, FALSE, NOW(), NOW()),
       ('백엔드팀 스크럼',    'GROUP',        @company, @admin, FALSE, FALSE, NOW(), NOW()),
       ('alice ↔ bob 1:1',    'DIRECT',       @company, @alice, FALSE, FALSE, NOW(), NOW());

SET @room_notice := (SELECT id FROM mc_chat_room WHERE name='HR 공지방'         ORDER BY id DESC LIMIT 1);
SET @room_group  := (SELECT id FROM mc_chat_room WHERE name='백엔드팀 스크럼'    ORDER BY id DESC LIMIT 1);
SET @room_direct := (SELECT id FROM mc_chat_room WHERE name='alice ↔ bob 1:1'   ORDER BY id DESC LIMIT 1);

-- ----------------------------------------------------------------
-- 2. 참여자
-- ----------------------------------------------------------------
-- ANNOUNCEMENT : admin=OWNER, 전 직원이 구독
INSERT INTO mc_chat_participant(chat_room_id, member_id, role, status, created_at, updated_at)
VALUES (@room_notice, @admin, 'OWNER',     'JOINED', NOW(), NOW()),
       (@room_notice, @alice, 'MEMBER',    'JOINED', NOW(), NOW()),
       (@room_notice, @bob,   'MEMBER',    'JOINED', NOW(), NOW()),
       (@room_notice, @carol, 'MEMBER',    'JOINED', NOW(), NOW());

-- GROUP : admin=OWNER, alice=MODERATOR, bob/carol=MEMBER
INSERT INTO mc_chat_participant(chat_room_id, member_id, role, status, created_at, updated_at)
VALUES (@room_group,  @admin, 'OWNER',     'JOINED', NOW(), NOW()),
       (@room_group,  @alice, 'MODERATOR', 'JOINED', NOW(), NOW()),
       (@room_group,  @bob,   'MEMBER',    'JOINED', NOW(), NOW()),
       (@room_group,  @carol, 'MEMBER',    'JOINED', NOW(), NOW());

-- DIRECT : alice + bob (둘 다 OWNER 로 동등하게)
INSERT INTO mc_chat_participant(chat_room_id, member_id, role, status, created_at, updated_at)
VALUES (@room_direct, @alice, 'OWNER',     'JOINED', NOW(), NOW()),
       (@room_direct, @bob,   'OWNER',     'JOINED', NOW(), NOW());

-- ----------------------------------------------------------------
-- 3. 메시지
-- ----------------------------------------------------------------
-- ANNOUNCEMENT : admin 이 NOTICE 로 공지
INSERT INTO mc_chat_message(chat_room_id, sender_id, type, content, client_message_id, created_at, updated_at)
VALUES (@room_notice, @admin, 'NOTICE', '[공지] 내일 전사 워크샵 14:00 대회의실',
        CONCAT('seed-notice-', UUID()), NOW(), NOW());

-- GROUP : 일반 대화 + 편집된 메시지 + 삭제된 메시지 + 답글
INSERT INTO mc_chat_message(chat_room_id, sender_id, type, content, client_message_id, created_at, updated_at)
VALUES (@room_group, @alice, 'TEXT', '좋은 아침입니다!', CONCAT('seed-g1-', UUID()), NOW(), NOW());
SET @msg_alice1 := LAST_INSERT_ID();

INSERT INTO mc_chat_message(chat_room_id, sender_id, type, content, client_message_id, edited, edited_at, created_at, updated_at)
VALUES (@room_group, @bob,   'TEXT', '수정된 내용입니다 (원본: 오타였음)',
        CONCAT('seed-g2-', UUID()), TRUE, NOW(), NOW(), NOW());
SET @msg_bob1 := LAST_INSERT_ID();

-- 위 편집된 메시지의 원본을 revision 에 남김
INSERT INTO mc_chat_message_revision(message_id, action, previous_content, actor_id, created_at, updated_at)
VALUES (@msg_bob1, 'EDIT', '오타였음', @bob, NOW(), NOW());

INSERT INTO mc_chat_message(chat_room_id, sender_id, type, content, client_message_id, deleted, created_at, updated_at)
VALUES (@room_group, @carol, 'TEXT', '', CONCAT('seed-g3-', UUID()), TRUE, NOW(), NOW());
SET @msg_carol_deleted := LAST_INSERT_ID();

INSERT INTO mc_chat_message_revision(message_id, action, previous_content, actor_id, created_at, updated_at)
VALUES (@msg_carol_deleted, 'DELETE', '부적절한 메시지', @carol, NOW(), NOW());

-- 답글
INSERT INTO mc_chat_message(chat_room_id, sender_id, type, content, client_message_id, reply_to_id, created_at, updated_at)
VALUES (@room_group, @alice, 'TEXT', '동의합니다!', CONCAT('seed-g4-', UUID()), @msg_alice1, NOW(), NOW());

-- DIRECT : 왕복 대화
INSERT INTO mc_chat_message(chat_room_id, sender_id, type, content, client_message_id, created_at, updated_at)
VALUES (@room_direct, @alice, 'TEXT', '밥 드셨어요?',       CONCAT('seed-d1-', UUID()), NOW(), NOW()),
       (@room_direct, @bob,   'TEXT', '네 방금요 ㅎㅎ',      CONCAT('seed-d2-', UUID()), NOW(), NOW()),
       (@room_direct, @alice, 'TEXT', '저는 아직이에요',     CONCAT('seed-d3-', UUID()), NOW(), NOW());

-- ----------------------------------------------------------------
-- 4. 읽음 영수증 (GROUP 방 기준 최신 메시지까지 읽음 처리)
-- ----------------------------------------------------------------
SET @last_group_msg := (SELECT MAX(id) FROM mc_chat_message WHERE chat_room_id=@room_group);

INSERT INTO mc_chat_read_receipt(room_id, message_id, member_id, device_id, created_at, updated_at)
VALUES (@room_group, @last_group_msg, @alice, 'web-1',    NOW(), NOW()),
       (@room_group, @last_group_msg, @bob,   'mobile-1', NOW(), NOW());

-- participant.last_read_message_id 갱신 (단조 증가 정책 반영)
UPDATE mc_chat_participant
   SET last_read_message_id = @last_group_msg
 WHERE chat_room_id = @room_group
   AND member_id IN (@alice, @bob);

-- ----------------------------------------------------------------
-- 5. 감사 로그 샘플
-- ----------------------------------------------------------------
INSERT INTO mc_chat_audit_log(actor_id, action, target_type, target_id, ip, user_agent, metadata, created_at, updated_at)
VALUES (@admin, 'ROOM_CREATED',            'ROOM',    @room_group,  '127.0.0.1', 'seed-script', '{"note":"seed"}', NOW(), NOW()),
       (@alice, 'MESSAGE_SENT',            'MESSAGE', @msg_alice1,  '127.0.0.1', 'seed-script', NULL,              NOW(), NOW()),
       (@bob,   'MESSAGE_EDITED',          'MESSAGE', @msg_bob1,    '127.0.0.1', 'seed-script', NULL,              NOW(), NOW()),
       (@carol, 'MESSAGE_DELETED',         'MESSAGE', @msg_carol_deleted, '127.0.0.1', 'seed-script', NULL,        NOW(), NOW());

-- ----------------------------------------------------------------
-- 6. Legal Hold 샘플 (GROUP 방에 활성 hold 적용)
--   - 해제된 hold 1건 + 활성 hold 1건
-- ----------------------------------------------------------------
INSERT INTO mc_chat_legal_hold(room_id, reason, case_id, start_at, released, created_at, updated_at)
VALUES (@room_group, '(해제됨) 초기 보전 조치', 'CASE-2025-001', DATE_SUB(NOW(), INTERVAL 30 DAY), TRUE,  NOW(), NOW()),
       (@room_group, '(활성) 내부 감사 진행중',  'CASE-2026-001', NOW(),                            FALSE, NOW(), NOW());

-- 활성 hold 가 있으므로 방 legal_hold 플래그도 true 로
UPDATE mc_chat_room SET legal_hold = TRUE WHERE id = @room_group;

-- ================================================================
-- 7. 검증용 SELECT (실행 후 확인)
-- ================================================================
SELECT '=== 방 목록 ===' AS section;
SELECT id, name, type, legal_hold, deleted FROM mc_chat_room;

SELECT '=== 참여자 ===' AS section;
SELECT chat_room_id, HEX(member_id) AS member_id, role, status, last_read_message_id
  FROM mc_chat_participant
 ORDER BY chat_room_id, id;

SELECT '=== 메시지 ===' AS section;
SELECT id, chat_room_id, HEX(sender_id) AS sender, type,
       IF(deleted, '(deleted)', content) AS content,
       edited, reply_to_id, created_at
  FROM mc_chat_message
 ORDER BY id;

SELECT '=== 메시지 리비전 ===' AS section;
SELECT message_id, action, previous_content, HEX(actor_id) AS actor, created_at
  FROM mc_chat_message_revision
 ORDER BY id;

SELECT '=== 읽음 영수증 ===' AS section;
SELECT room_id, message_id, HEX(member_id) AS member, device_id
  FROM mc_chat_read_receipt
 ORDER BY id;

SELECT '=== 감사 로그 ===' AS section;
SELECT HEX(actor_id) AS actor, action, target_type, target_id, created_at
  FROM mc_chat_audit_log
 ORDER BY id;

SELECT '=== Legal Hold ===' AS section;
SELECT id, room_id, case_id, reason, released, start_at, end_at
  FROM mc_chat_legal_hold
 ORDER BY id;
