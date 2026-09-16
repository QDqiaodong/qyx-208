package com.example.geological.service.impl;

import com.example.geological.dto.TransferNotificationDTO;
import com.example.geological.entity.SurveyTeam;
import com.example.geological.entity.TransferNotification;
import com.example.geological.entity.Workstation;
import com.example.geological.entity.WorkstationTransfer;
import com.example.geological.repository.SurveyTeamRepository;
import com.example.geological.repository.TransferNotificationRepository;
import com.example.geological.service.TransferNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferNotificationServiceImpl implements TransferNotificationService {

    private final TransferNotificationRepository notificationRepository;
    private final SurveyTeamRepository surveyTeamRepository;

    @Override
    @Transactional
    public void createTransferNotifications(WorkstationTransfer transfer) {
        SurveyTeam fromTeam = transfer.getFromTeam();
        SurveyTeam toTeam = transfer.getToTeam();
        Workstation workstation = transfer.getWorkstation();

        if (fromTeam == null || toTeam == null || workstation == null) {
            throw new IllegalArgumentException("转队通知缺少原小队、新小队或操作台信息");
        }

        String workstationNo = workstation.getWorkstationNo();
        TransferNotification outNotification = buildNotification(
                transfer, fromTeam, workstation, TransferNotification.DIRECTION_OUT,
                workstationNo, toTeam.getTeamName(),
                "操作台调出通知",
                String.format("操作台[%s]已从本小队调走，调入小队：%s。", workstationNo, toTeam.getTeamName())
        );
        TransferNotification inNotification = buildNotification(
                transfer, toTeam, workstation, TransferNotification.DIRECTION_IN,
                workstationNo, fromTeam.getTeamName(),
                "操作台调入通知",
                String.format("操作台[%s]已调入本小队，原小队：%s。", workstationNo, fromTeam.getTeamName())
        );

        notificationRepository.saveAll(List.of(outNotification, inNotification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferNotificationDTO> getByTeamId(Long teamId, Boolean unreadOnly) {
        assertTeamExists(teamId);
        List<TransferNotification> notifications = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findUnreadDetailedByTeamId(teamId)
                : notificationRepository.findDetailedByTeamId(teamId);
        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long teamId) {
        assertTeamExists(teamId);
        return notificationRepository.countByRecipientTeamIdAndReadStatus(teamId, TransferNotification.UNREAD);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countAllUnread() {
        Map<Long, Long> counts = new LinkedHashMap<>();
        surveyTeamRepository.findByStatus(1).forEach(team ->
                counts.put(team.getId(), notificationRepository
                        .countByRecipientTeamIdAndReadStatus(team.getId(), TransferNotification.UNREAD))
        );
        return counts;
    }

    @Override
    @Transactional
    public void markAsRead(Long teamId, Long notificationId) {
        assertTeamExists(teamId);
        int updated = notificationRepository.markAsRead(notificationId, teamId, LocalDateTime.now());
        if (updated == 0) {
            TransferNotification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new IllegalArgumentException("通知不存在: " + notificationId));
            if (!notification.getRecipientTeam().getId().equals(teamId)) {
                throw new IllegalArgumentException("通知不属于该小队: " + notificationId);
            }
        }
    }

    @Override
    @Transactional
    public long markAllAsRead(Long teamId) {
        assertTeamExists(teamId);
        return notificationRepository.markAllAsRead(teamId, LocalDateTime.now());
    }

    private TransferNotification buildNotification(
            WorkstationTransfer transfer,
            SurveyTeam recipientTeam,
            Workstation workstation,
            Integer direction,
            String workstationNo,
            String counterpartTeamName,
            String title,
            String content
    ) {
        TransferNotification notification = new TransferNotification();
        notification.setTransfer(transfer);
        notification.setRecipientTeam(recipientTeam);
        notification.setWorkstation(workstation);
        notification.setDirection(direction);
        notification.setWorkstationNo(workstationNo);
        notification.setCounterpartTeamName(counterpartTeamName);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadStatus(TransferNotification.UNREAD);
        return notification;
    }

    private void assertTeamExists(Long teamId) {
        if (!surveyTeamRepository.existsById(teamId)) {
            throw new IllegalArgumentException("小队不存在: " + teamId);
        }
    }

    private TransferNotificationDTO convertToDTO(TransferNotification notification) {
        TransferNotificationDTO dto = new TransferNotificationDTO();
        dto.setId(notification.getId());
        dto.setTeamId(notification.getRecipientTeam().getId());
        dto.setTeamName(notification.getRecipientTeam().getTeamName());
        dto.setWorkstationId(notification.getWorkstation().getId());
        dto.setWorkstationNo(notification.getWorkstationNo());
        dto.setTransferId(notification.getTransfer().getId());
        dto.setDirection(notification.getDirection());
        dto.setCounterpartTeamName(notification.getCounterpartTeamName());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setReadStatus(notification.getReadStatus());
        dto.setReadTime(notification.getReadTime());
        dto.setCreateTime(notification.getCreateTime());
        return dto;
    }
}
