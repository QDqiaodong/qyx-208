package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 营地柴油桶档案。
 *
 * 油桶不属于任何小队的操作台/样品袋资产：不挂小队、不计入小队承重，
 * 余量只跟桶走。所有领用扣减、回灌增加都在 {@link FuelLedger} 落账的同时
 * 在本实体的 current_level 上即时落库（桶行悲观锁串行化），
 * 关闭页面重新打开读到的仍是扣减/回灌后的余量。
 */
@Entity
@Table(name = "fuel_barrel")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuelBarrel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 桶号（桶上贴的数字，全营唯一） */
    @Column(name = "barrel_no", nullable = false, unique = true, length = 50)
    private String barrelNo;

    /** 额定升数（满桶容量） */
    @Column(name = "rated_capacity", nullable = false)
    private Double ratedCapacity;

    /** 当前余量（升），始终满足 0 <= current_level <= rated_capacity */
    @Column(name = "current_level", nullable = false)
    private Double currentLevel;

    @Column(name = "remark", length = 200)
    private String remark;

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
