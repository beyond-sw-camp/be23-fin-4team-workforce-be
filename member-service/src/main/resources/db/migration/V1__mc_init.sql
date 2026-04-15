-- member-chat 전용 스키마 (prefix mc_)
-- 기존 chat-ref 테이블과 이름이 겹치지 않도록 설계됨.

CREATE TABLE IF NOT EXISTS mc_chat_room (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    company_id      BINARY(16)   NOT NULL,
    created_by      BINARY(16)   NOT NULL,
    image_url       VARCHAR(500),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME NOT NULL,
    INDEX idx_mc_room_company (company_id),
    INDEX idx_mc_room_deleted (deleted)
);

CREATE TABLE IF NOT EXISTS mc_chat_participant (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id           BIGINT NOT NULL,
    member_id              BINARY(16) NOT NULL,
    role                   VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status                 VARCHAR(20) NOT NULL DEFAULT 'JOINED',
    start_message_id       BIGINT,
    last_read_message_id   BIGINT,
    muted_until            DATETIME,
    created_at             DATETIME NOT NULL,
    updated_at             DATETIME NOT NULL,
    CONSTRAINT uq_mc_participant UNIQUE (chat_room_id, member_id),
    CONSTRAINT fk_mc_participant_room FOREIGN KEY (chat_room_id) REFERENCES mc_chat_room(id),
    INDEX idx_mc_participant_member (member_id, status)
);

CREATE TABLE IF NOT EXISTS mc_chat_message (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id       BIGINT NOT NULL,
    sender_id          BINARY(16) NOT NULL,
    type               VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    content            VARCHAR(4000) NOT NULL,
    client_message_id  VARCHAR(64),
    reply_to_id        BIGINT,
    deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    edited             BOOLEAN NOT NULL DEFAULT FALSE,
    edited_at          DATETIME,
    created_at         DATETIME NOT NULL,
    updated_at         DATETIME NOT NULL,
    CONSTRAINT uq_mc_msg_client_id UNIQUE (client_message_id),
    CONSTRAINT fk_mc_msg_room FOREIGN KEY (chat_room_id) REFERENCES mc_chat_room(id),
    INDEX idx_mc_msg_room_id (chat_room_id, id),
    INDEX idx_mc_msg_room_created (chat_room_id, created_at)
);

CREATE TABLE IF NOT EXISTS mc_chat_message_image (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_message_id  BIGINT NOT NULL,
    url              VARCHAR(1000) NOT NULL,
    mime_type        VARCHAR(100),
    size_bytes       BIGINT,
    scan_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING_SCAN',
    CONSTRAINT fk_mc_msg_img FOREIGN KEY (chat_message_id) REFERENCES mc_chat_message(id)
);

CREATE TABLE IF NOT EXISTS mc_chat_message_revision (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id         BIGINT NOT NULL,
    action             VARCHAR(20) NOT NULL,
    previous_content   VARCHAR(4000),
    actor_id           BINARY(16) NOT NULL,
    created_at         DATETIME NOT NULL,
    updated_at         DATETIME NOT NULL,
    INDEX idx_mc_rev_message (message_id)
);

CREATE TABLE IF NOT EXISTS mc_chat_read_receipt (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id     BIGINT NOT NULL,
    message_id  BIGINT NOT NULL,
    member_id   BINARY(16) NOT NULL,
    device_id   VARCHAR(100),
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL,
    CONSTRAINT uq_mc_read UNIQUE (message_id, member_id),
    INDEX idx_mc_read_member (member_id, message_id)
);

CREATE TABLE IF NOT EXISTS mc_chat_audit_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id     BINARY(16) NOT NULL,
    action       VARCHAR(40) NOT NULL,
    target_type  VARCHAR(30) NOT NULL,
    target_id    BIGINT,
    ip           VARCHAR(64),
    user_agent   VARCHAR(512),
    metadata     TEXT,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NOT NULL,
    INDEX idx_mc_audit_actor (actor_id, created_at),
    INDEX idx_mc_audit_target (target_type, target_id, created_at)
);

CREATE TABLE IF NOT EXISTS mc_chat_legal_hold (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id     BIGINT,
    member_id   BINARY(16),
    reason      VARCHAR(500) NOT NULL,
    case_id     VARCHAR(100),
    start_at    DATETIME NOT NULL,
    end_at      DATETIME,
    released    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL,
    INDEX idx_mc_hold_room (room_id, released),
    INDEX idx_mc_hold_member (member_id, released)
);
