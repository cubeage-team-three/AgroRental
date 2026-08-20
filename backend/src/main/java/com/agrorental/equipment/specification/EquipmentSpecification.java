package com.agrorental.equipment.specification;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.equipment.dto.EquipmentSearchRequest;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.review.entity.Review;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA Specification builder for dynamic multi-criteria Equipment filtering.
 * Constructs database-side Criteria API predicates based on search request parameters.
 */
public class EquipmentSpecification {

    /**
     * Builds a composable Specification for Equipment based on the provided search request filters.
     * Enforces public discovery rules (isDisabled = false) and ignores null/blank criteria.
     *
     * @param request Search filter DTO containing optional query criteria
     * @return Specification<Equipment> for JpaSpecificationExecutor query execution
     */
    public static Specification<Equipment> buildSpecification(EquipmentSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always enforce non-disabled state for public discovery
            predicates.add(cb.isFalse(root.get("isDisabled")));

            if (request != null) {
                // Filter by Equipment Category
                if (request.getCategory() != null) {
                    predicates.add(cb.equal(root.get("category"), request.getCategory()));
                }

                // Filter by Minimum Rental Price (rentalPrice >= minPrice)
                if (request.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("rentalPrice"), request.getMinPrice()));
                }

                // Filter by Maximum Rental Price (rentalPrice <= maxPrice)
                if (request.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("rentalPrice"), request.getMaxPrice()));
                }

                // Filter by Availability Status
                if (request.getAvailabilityStatus() != null) {
                    predicates.add(cb.equal(root.get("availabilityStatus"), request.getAvailabilityStatus()));
                }

                // Filter by Location Address (case-insensitive partial match)
                if (StringUtils.hasText(request.getLocationAddress())) {
                    String pattern = "%" + request.getLocationAddress().trim().toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("locationAddress")), pattern));
                }

                // Filter by Minimum Horse Power (numeric capacity >= minHp)
                if (request.getMinHp() != null) {
                    Expression<String> digitsOnly = cb.function("REGEXP_SUBSTR", String.class, root.get("capacity"), cb.literal("[0-9]+"));
                    Expression<Integer> numericHp = digitsOnly.as(Integer.class);
                    predicates.add(cb.greaterThanOrEqualTo(numericHp, request.getMinHp()));
                }

                // FR-16 / FR-05: Future Availability Date Range & Overlap Exclusion
                if (request.getStartDate() != null || request.getEndDate() != null) {
                    LocalDate start = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
                    LocalDate end = request.getEndDate() != null ? request.getEndDate() : start;

                    // Future availability bounds validation: availableFromDate <= start AND availableToDate >= end
                    Predicate fromNull = cb.isNull(root.get("availableFromDate"));
                    Predicate fromValid = cb.lessThanOrEqualTo(root.get("availableFromDate"), start);
                    predicates.add(cb.or(fromNull, fromValid));

                    Predicate toNull = cb.isNull(root.get("availableToDate"));
                    Predicate toValid = cb.greaterThanOrEqualTo(root.get("availableToDate"), end);
                    predicates.add(cb.or(toNull, toValid));

                    // Dynamic overlap exclusion: Exclude machinery with active bookings in [start, end]
                    Subquery<Long> bookedSubquery = query.subquery(Long.class);
                    Root<Booking> bookingRoot = bookedSubquery.from(Booking.class);
                    bookedSubquery.select(bookingRoot.get("equipment").get("id"));
                    bookedSubquery.where(
                            cb.equal(bookingRoot.get("equipment").get("id"), root.get("id")),
                            bookingRoot.get("status").in(List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)),
                            cb.lessThanOrEqualTo(bookingRoot.get("startDate"), end),
                            cb.greaterThanOrEqualTo(bookingRoot.get("endDate"), start)
                    );
                    predicates.add(cb.not(root.get("id").in(bookedSubquery)));
                }

                // FR-05: Minimum Rating Filter via Review Read-Only Subquery
                if (request.getMinRating() != null && request.getMinRating() > 0.0) {
                    Subquery<Double> ratingSubquery = query.subquery(Double.class);
                    Root<Review> reviewRoot = ratingSubquery.from(Review.class);
                    ratingSubquery.select(cb.avg(reviewRoot.get("rating")));
                    ratingSubquery.where(cb.equal(reviewRoot.get("equipmentId"), root.get("id")));
                    predicates.add(cb.greaterThanOrEqualTo(ratingSubquery, request.getMinRating()));
                }

                // FR-05: Haversine Radial Distance Calculation (maxDistanceKm)
                if (request.getUserLat() != null && request.getUserLng() != null && request.getMaxDistanceKm() != null && request.getMaxDistanceKm() > 0.0) {
                    double uLatRad = Math.toRadians(request.getUserLat());
                    double uLngRad = Math.toRadians(request.getUserLng());

                    Expression<Double> latRad = cb.function("RADIANS", Double.class, root.get("latitude"));
                    Expression<Double> lngRad = cb.function("RADIANS", Double.class, root.get("longitude"));

                    Expression<Double> cosUserLat = cb.literal(Math.cos(uLatRad));
                    Expression<Double> sinUserLat = cb.literal(Math.sin(uLatRad));

                    Expression<Double> cosEquipLat = cb.function("COS", Double.class, latRad);
                    Expression<Double> sinEquipLat = cb.function("SIN", Double.class, latRad);

                    Expression<Double> lngDiff = cb.diff(lngRad, cb.literal(uLngRad));
                    Expression<Double> cosLngDiff = cb.function("COS", Double.class, lngDiff);

                    Expression<Double> cosProd = cb.prod(cosUserLat, cb.prod(cosEquipLat, cosLngDiff));
                    Expression<Double> sinProd = cb.prod(sinUserLat, sinEquipLat);
                    Expression<Double> sum = cb.sum(cosProd, sinProd);

                    Expression<Double> acos = cb.function("ACOS", Double.class, sum);
                    Expression<Double> distKm = cb.prod(cb.literal(6371.0), acos);

                    predicates.add(cb.lessThanOrEqualTo(distKm, request.getMaxDistanceKm()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification helper for rental price range filtering.
     */
    public static Specification<Equipment> hasPriceBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("rentalPrice"), minPrice, maxPrice);
            } else if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("rentalPrice"), minPrice);
            } else if (maxPrice != null) {
                return cb.lessThanOrEqualTo(root.get("rentalPrice"), maxPrice);
            }
            return cb.conjunction();
        };
    }

    /**
     * Specification helper for minimum horse power filtering.
     */
    public static Specification<Equipment> hasMinHp(Integer minHp) {
        return (root, query, cb) -> {
            if (minHp != null) {
                return cb.greaterThanOrEqualTo(root.get("hp"), minHp);
            }
            return cb.conjunction();
        };
    }

    /**
     * Specification helper for location partial string matching.
     */
    public static Specification<Equipment> locationContains(String location) {
        return (root, query, cb) -> {
            if (StringUtils.hasText(location)) {
                String pattern = "%" + location.trim().toLowerCase() + "%";
                return cb.like(cb.lower(root.get("locationAddress")), pattern);
            }
            return cb.conjunction();
        };
    }
}
