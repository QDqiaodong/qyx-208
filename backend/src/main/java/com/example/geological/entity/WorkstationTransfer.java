package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workstation_transfer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkstationTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workstation_id", nullable = false)
    private Workstation workstation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_team_id")
    private SurveyTeam fromTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_team_id", nullable = false)
    private SurveyTeam toTeam;

    @Column(name = "transfer_reason", length = 500)
    private String transferReason;

    @Column(name = "operator", length = 50)
    private String operator;

    @Column(name = "transfer_time", nullable = false)
    private LocalDateTime transferTime;

    @PrePersist
    protected void onCreate() {
        transferTime = LocalDateTime.now();
    }
}