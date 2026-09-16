package com.example.geological.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransferNotificationDTO {

    private Long id;
    private Long teamId;
    private String teamName;
    private Long workstationId;
    private String workstationNo;
    private Long transferId;
    /** 1=调出通知；2=调入通知 */
    private Integer direction;
    private String counterpartTeamName;
    private String title;
    private String content;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
