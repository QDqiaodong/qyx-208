package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workstation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Workstation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workstation_no", nullable = false, unique = true, length = 50)
    private String workstationNo;

    @Column(name = "load_capacity", nullable = false)
    private Double loadCapacity;

    @Column(name = "workstation_type", length = 100)
    private String workstationType;

    /**
     * 适配工作站名字。
     * 占用规则：同一时刻同一站名只允许一台在用(status=1)操作台占用。
     * 停用台、空站名不占站。并发放置由生成列 active_adapt_station 上的唯一索引
     * uk_workstation_active_adapt_station 兜底（见 WorkstationSchemaInitializer），
     * Service 层保存前另做占用预检。
     */
    @Column(name = "adapt_station", length = 200)
    private String adaptStation;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_team_id")
    private SurveyTeam currentTeam;

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