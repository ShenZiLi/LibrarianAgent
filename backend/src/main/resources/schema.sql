CREATE DATABASE IF NOT EXISTS librarian DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE librarian;

CREATE TABLE IF NOT EXISTS document_record (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL,
    file_name   VARCHAR(512) NOT NULL,
    file_type   VARCHAR(128),
    file_size   BIGINT DEFAULT 0,
    status      VARCHAR(32) NOT NULL DEFAULT 'processing',
    chunk_count INT DEFAULT 0,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL,
    deleted     INT DEFAULT 0,
    UNIQUE KEY uk_document_id (document_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
