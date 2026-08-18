package com.agrorental.operator.repository;

import com.agrorental.operator.entity.JobStatus;
import com.agrorental.operator.entity.OperatorJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperatorJobRepository extends JpaRepository<OperatorJob, Long> {

    List<OperatorJob> findAllByOperatorIdOrderByScheduledDateDescCreatedAtDesc(Long operatorId);

    List<OperatorJob> findAllByOperatorIdAndStatusOrderByScheduledDateDescCreatedAtDesc(Long operatorId, JobStatus status);

    Optional<OperatorJob> findByIdAndOperatorId(Long id, Long operatorId);

    long countByOperatorId(Long operatorId);

    long countByOperatorIdAndStatus(Long operatorId, JobStatus status);
}
