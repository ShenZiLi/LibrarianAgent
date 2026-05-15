package com.librarian.repository;

import com.librarian.model.entity.EvaluationResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResultEntity, String> {
}
