package com.agrorental.notification.repository;

import com.agrorental.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing Notification persistence.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(String recipientRole, Long recipientId);

    List<Notification> findByRecipientRoleAndRecipientIdAndIsReadFalseOrderByCreatedAtDesc(String recipientRole, Long recipientId);

    long countByRecipientRoleAndRecipientIdAndIsReadFalse(String recipientRole, Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientRole = :recipientRole AND n.recipientId = :recipientId")
    void markAllAsRead(@Param("recipientRole") String recipientRole, @Param("recipientId") Long recipientId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientRole = :recipientRole AND n.recipientId = :recipientId")
    void deleteAllByRecipient(@Param("recipientRole") String recipientRole, @Param("recipientId") Long recipientId);
}
