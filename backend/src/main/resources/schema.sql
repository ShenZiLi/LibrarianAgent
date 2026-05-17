CREATE DATABASE IF NOT EXISTS librarian DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE librarian;

CREATE TABLE IF NOT EXISTS document_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id   VARCHAR(64) NOT NULL,
    file_name     VARCHAR(512) NOT NULL,
    file_type     VARCHAR(128),
    file_size     BIGINT DEFAULT 0,
    status        VARCHAR(32) NOT NULL DEFAULT 'processing',
    chunk_count   INT DEFAULT 0,
    error_message VARCHAR(1024),
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME NOT NULL,
    deleted       INT DEFAULT 0,
    UNIQUE KEY uk_document_id (document_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_chunk (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id  VARCHAR(64) NOT NULL,
    chunk_index  INT NOT NULL,
    chunk_text   TEXT,
    vector_id    VARCHAR(128) NOT NULL,
    created_at   DATETIME NOT NULL,
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS query_log (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    query               VARCHAR(1024) NOT NULL,
    retrieved_docs      INT NOT NULL DEFAULT 0,
    avg_similarity      DOUBLE NOT NULL DEFAULT 0,
    retrieval_time_ms   BIGINT NOT NULL DEFAULT 0,
    generation_time_ms  BIGINT NOT NULL DEFAULT 0,
    created_at          DATETIME NOT NULL,
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
