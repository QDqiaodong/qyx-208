package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 气象卡片：每个自然日一行，当日合计毫米数。
 *
 * 合计 = 该自然日各收集桶有效(status=1)读数之和，作废读数不计入。
 * 合计不落笔在别处、不增量累加：每次记一笔/改数/作废，都在同一事务内
 * 按当时的有效读数重算本行并落库——读数改动和卡片合计要么一起生效，
 * 要么一起回滚，不存在「只改了桶上的数、卡片还是旧合计」的中间态，
 * 也不会出现做到一半中断后卡片停在半新不旧的数。
 */
@Entity
@Table(name = "rain_daily_total")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RainDailyTotal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 自然日（一天一张卡片，全表唯一） */
    @Column(name = "read_date", nullable = false, unique = true)
    private LocalDate readDate;

    /** 当日合计毫米数 = 该日各桶有效读数之和 */
    @Column(name = "total_mm", nullable = false)
    private Double totalMm;

    /** 卡片最近一次重算时间 */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    protected void onTouch() {
        updateTime = LocalDateTime.now();
    }
}
