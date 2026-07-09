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