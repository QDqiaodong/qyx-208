package com.example.geological.repository;

import com.example.geological.entity.TransferNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransferNotificationRepository extends JpaRepository<TransferNotification, Long> {

    @Query("SELECT n FROM TransferNotification n " +
            "JOIN FETCH n.recipientTeam t " +
            "JOIN FETCH n.workstation w " +
            "JOIN FETCH n.transfer x " +
            "WHERE t.id = :teamId " +
            "ORDER BY n.createTime DESC, n.id DESC")
    List<TransferNotification> findDetailedByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT n FROM TransferNotification n " +
            "JOIN FETCH n.recipientTeam t " +
            "JOIN FETCH n.workstation w " +
            "JOIN FETCH n.transfer x " +
            "WHERE t.id = :teamId AND n.readStatus = 0 " +
            "ORDER BY n.createTime DESC, n.id DESC")
    List<TransferNotification> findUnreadDetailedByTeamId(@Param("teamId") Long teamId);

    long countByRecipientTeamIdAndReadStatus(Long teamId, Integer readStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TransferNotification n " +
            "SET n.readStatus = 1, n.readTime = :readTime " +
            "WHERE n.id = :id AND n.recipientTeam.id = :teamId AND n.readStatus = 0")
    int markAsRead(@Param("id") Long id, @Param("teamId") Long teamId, @Param("readTime") LocalDateTime readTime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TransferNotification n " +
            "SET n.readStatus = 1, n.readTime = :readTime " +
            "WHERE n.recipientTeam.id = :teamId AND n.readStatus = 0")
    int markAllAsRead(@Param("teamId") Long teamId, @Param("readTime") LocalDateTime readTime);
}
