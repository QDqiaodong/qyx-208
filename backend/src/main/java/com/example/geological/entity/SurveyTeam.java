package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "survey_team")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_name", nullable = false, unique = true, length = 100)
    private String teamName;

    @Column(name = "team_code", nullable = false, unique = true, length = 50)
    private String teamCode;

    @Column(name = "leader_name", length = 50)
    private String leaderName;

    @Column(name = "leader_phone", length = 20)
    private String leaderPhone;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * 随队总承重上限(kg)，后勤核定，只能在小队档案中单独修改
     */
    @Column(name = "max_load_capacity", nullable = false)
    private Double maxLoadCapacity = 0.0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> members = new ArrayList<>();

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