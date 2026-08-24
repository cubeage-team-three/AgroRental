package com.agrorental.operator.repository;

import com.agrorental.operator.entity.OperatorJobPauseInterval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorJobPauseIntervalRepository extends JpaRepository<OperatorJobPauseInterval, Long> {

    List<OperatorJobPauseInterval> findByAssignmentIdOrderByPausedAtAsc(Long assignmentId);

    Optional<OperatorJobPauseInterval> findTopByAssignmentIdAndResumedAtIsNullOrderByPausedAtDesc(Long assignmentId);

    List<OperatorJobPauseInterval> findByOperatorId(Long operatorId);
}
