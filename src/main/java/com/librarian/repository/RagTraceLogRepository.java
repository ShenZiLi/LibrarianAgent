package com.librarian.repository;

import com.librarian.model.entity.RagTraceLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RagTraceLogRepository extends JpaRepository<RagTraceLogEntity, Long> {

    Optional<RagTraceLogEntity> findByTraceId(String traceId);
}
