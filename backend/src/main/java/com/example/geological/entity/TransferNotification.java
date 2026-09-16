package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "transfer_notification",
        indexes = {
                @Index(name = "idx_transfer_notification_team_read", columnList = "recipient_team_id,read_status"),
                @Index(name = "idx_transfer_notification_team_time", columnList = "recipient_team_id,create_time")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferNotification {

    public static final int DIRECTION_OUT = 1;
    public static final int DIRECTION_IN = 2;
    public static final int UNREAD = 0;
    public static final int READ = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 接收通知的小队。一次转队生成原队、新队两行，读状态互不影响。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_team_id", nullable = false)
    private SurveyTeam recipientTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workstation_id", nullable = false)
    private Workstation workstation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private WorkstationTransfer transfer;

    /** 1=调出（原小队）；2=调入（新小队） */
    @Column(name = "direction", nullable = false)
    private Integer direction;

    /** 转队发生时的操作台编号，避免后续档案改名影响历史通知 */
    @Column(name = "workstation_no", nullable = false, length = 50)
    private String workstationNo;

    /** 对方小队：调出通知里为新小队，调入通知里为原小队 */
    @Column(name = "counterpart_team_name", nullable = false, length = 100)
    private String counterpartTeamName;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "read_status", nullable = false)
    private Integer readStatus = UNREAD;

    @Column(name = "read_time")
    private LocalDateTime readTime;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
