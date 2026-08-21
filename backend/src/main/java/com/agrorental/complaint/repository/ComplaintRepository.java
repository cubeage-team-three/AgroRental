package com.agrorental.complaint.repository;

import com.agrorental.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    List<Complaint> findByBookingId(Long bookingId);
}
