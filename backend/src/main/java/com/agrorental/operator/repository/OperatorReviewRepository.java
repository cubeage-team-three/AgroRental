package com.agrorental.operator.repository;

import com.agrorental.operator.entity.OperatorReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorReviewRepository extends JpaRepository<OperatorReview, Long> {

    boolean existsByAssignmentId(Long assignmentId);

    Optional<OperatorReview> findByAssignmentId(Long assignmentId);

    Page<OperatorReview> findByOperatorId(Long operatorId, Pageable pageable);

    List<OperatorReview> findByOperatorId(Long operatorId);

    long countByOperatorId(Long operatorId);
}
