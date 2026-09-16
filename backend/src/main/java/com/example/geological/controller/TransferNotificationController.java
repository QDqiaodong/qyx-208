package com.example.geological.controller;

import com.example.geological.dto.ResponseDTO;
import com.example.geological.dto.TransferNotificationDTO;
import com.example.geological.service.TransferNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransferNotificationController {

    private final TransferNotificationService transferNotificationService;

    @GetMapping("/teams/{teamId}/notifications")
    public ResponseDTO<List<TransferNotificationDTO>> getTeamNotifications(
            @PathVariable Long teamId,
            @RequestParam(required = false) Boolean unreadOnly
    ) {
        return ResponseDTO.success(transferNotificationService.getByTeamId(teamId, unreadOnly));
    }

    @GetMapping("/teams/{teamId}/notifications/unread-count")
    public ResponseDTO<Long> getUnreadCount(@PathVariable Long teamId) {
        return ResponseDTO.success(transferNotificationService.countUnread(teamId));
    }

    @PutMapping("/teams/{teamId}/notifications/{notificationId}/read")
    public ResponseDTO<Void> markAsRead(@PathVariable Long teamId, @PathVariable Long notificationId) {
        transferNotificationService.markAsRead(teamId, notificationId);
        return ResponseDTO.success("已读", null);
    }

    @PutMapping("/teams/{teamId}/notifications/read-all")
    public ResponseDTO<Long> markAllAsRead(@PathVariable Long teamId) {
        return ResponseDTO.success("全部已读", transferNotificationService.markAllAsRead(teamId));
    }

    @GetMapping("/notifications/unread-counts")
    public ResponseDTO<Map<Long, Long>> getAllUnreadCounts() {
        return ResponseDTO.success(transferNotificationService.countAllUnread());
    }
}
