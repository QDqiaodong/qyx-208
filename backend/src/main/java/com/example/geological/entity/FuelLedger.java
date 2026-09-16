package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 油料领用/回灌台账（单独的一张流水表，与操作台挂靠、转队通知、队员停用互不干涉）。
 *
 * type：1=领用出账（从桶里舀走，余量扣减）；2=回灌入账（没用完的油倒回同一桶，余量增加）。
 * 每一笔流水都带操作后桶余量快照 level_after，账面可对、可追。
 * 余量扣减/增加与台账落账在同一事务、同一油桶行悲观锁内原子完成：
 * 余额不足、回灌会超过额定升数时整笔回滚——台账不落、余量不动。
 */
@Entity
@Table(
    name = "fuel_ledger",
    indexes = {
        @Index(name = "idx_fuel_ledger_barrel", columnList = "barrel_id,ledger_date"),
        @Index(name = "idx_fuel_ledger_team", columnList = "team_id"),
        @Index(name = "idx_fuel_ledger_member", columnList = "member_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuelLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 油桶（余量只跟桶走，不挂小队资产） */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barrel_id", nullable = false)
    private FuelBarrel barrel;

    /** 领油小队；回灌且未关联原领用单时允许为空 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private SurveyTeam team;

    /** 领油队员；回灌且未关联原领用单时允许为空 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private TeamMember member;

    /** 流水类型：1=领用（舀走），2=回灌（倒回） */
    @Column(name = "type", nullable = false)
    private Integer type;

    /** 本次升数（恒为正数）；领用时为舀走量，回灌时为倒回量 */
    @Column(name = "liters", nullable = false)
    private Double liters;

    /** 登记日期（哪一天） */
    @Column(name = "ledger_date", nullable = false)
    private LocalDate ledgerDate;

    /** 回灌时关联的原领用台账 id；领用为空 */
    @Column(name = "related_issue_id")
    private Long relatedIssueId;

    /** 操作完成后该桶余量快照（升） */
    @Column(name = "level_after", nullable = false)
    private Double levelAfter;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
