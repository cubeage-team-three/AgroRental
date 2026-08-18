package com.agrorental.operator.repository;

import com.agrorental.operator.entity.OperatorWorkMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorWorkMilestoneRepository extends JpaRepository<OperatorWorkMilestone, Long> {

    List<OperatorWorkMilestone> findAllByJobIdOrderByCreatedAtAsc(Long jobId);

    List<OperatorWorkMilestone> findAllByJobIdAndOperatorIdOrderByCreatedAtAsc(Long jobId, Long operatorId);
}
