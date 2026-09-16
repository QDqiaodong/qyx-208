package com.example.geological.service;

import com.example.geological.dto.TransferNotificationDTO;
import com.example.geological.entity.WorkstationTransfer;

import java.util.List;
import java.util.Map;

public interface TransferNotificationService {

    /**
     * 一次成功转队生成两条互不相干的收件记录：原队一条调出、新队一条调入。
     */
    void createTransferNotifications(WorkstationTransfer transfer);

    List<TransferNotificationDTO> getByTeamId(Long teamId, Boolean unreadOnly);

    long countUnread(Long teamId);

    Map<Long, Long> countAllUnread();

    void markAsRead(Long teamId, Long notificationId);

    long markAllAsRead(Long teamId);
}
