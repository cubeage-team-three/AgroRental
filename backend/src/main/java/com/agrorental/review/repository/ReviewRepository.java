package com.agrorental.review.repository;

import com.agrorental.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for Review entity.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    List<Review> findByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);

    List<Review> findByPartnerIdOrderByCreatedAtDesc(Long partnerId);

    List<Review> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.equipmentId = :equipmentId")
    Double findAverageRatingByEquipmentId(@Param("equipmentId") Long equipmentId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.equipmentId = :equipmentId")
    long countByEquipmentId(@Param("equipmentId") Long equipmentId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.partnerId = :partnerId")
    Double findAverageRatingByPartnerId(@Param("partnerId") Long partnerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.partnerId = :partnerId")
    long countByPartnerId(@Param("partnerId") Long partnerId);
}
