package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 岩心样品袋送检登记台账。
 *
 * 状态流转：在途(IN_TRANSIT=1) ——办结(COMPLETED=2)；办结后可退回在途。
 * 唯一性：同一小队 + 同一送检日 + 同一袋号只能落一条（数据库唯一索引兜底），
 * 并发同交时由小队行级悲观锁 + 唯一索引共同保证只有先到的一条进账。
 */
@Entity
@Table(
    name = "sample_bag",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sample_bag_team_date_bagno",
        columnNames = {"team_id", "submit_date", "bag_no"}
    ),
    indexes = {
        @Index(name = "idx_sample_bag_team_status", columnList = "team_id,status"),
        @Index(name = "idx_sample_bag_member", columnList = "member_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SampleBag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 袋号 */
    @Column(name = "bag_no", nullable = false, length = 50)
    private String bagNo;

    /** 所属小队 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private SurveyTeam team;

    /** 送检队员 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private TeamMember member;

    /** 袋重(kg) */
    @Column(name = "bag_weight", nullable = false)
    private Double bagWeight;

    /** 送检日 */
    @Column(name = "submit_date", nullable = false)
    private LocalDate submitDate;

    /** 状态：1=在途，2=办结（出站） */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 办结时间（出站时间），在途时为空 */
    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
