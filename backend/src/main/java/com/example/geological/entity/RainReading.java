package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 雨量读数（rec 本上的每一笔）：哪个收集桶、哪个自然日、本次多少毫米。
 *
 * 同一桶同一自然日只留一条有效(status=1)读数：Service 在桶行悲观锁内查重，
 * 数据库侧由生成列唯一索引 uk_rain_reading_valid_bucket_day 兜底（作废行不参与唯一）。
 * 作废(status=2)只标记不删除：作废原因、作废时间永久留痕，当日合计不再计入这一笔。
 *
 * version 是改数/作废的凭据：值班在页面上看到的是哪个版本，提交时就得带哪个版本；
 * 两名值班抢着改同一桶同一日时，先提交的生效并把版本 +1，后提交的版本对不上即失败（409），
 * 不会出现两笔都生效、后到者悄悄覆盖先到者的情况。
 */
@Entity
@Table(
    name = "rain_reading",
    indexes = {
        @Index(name = "idx_rain_reading_bucket_date", columnList = "bucket_id,read_date"),
        @Index(name = "idx_rain_reading_date_status", columnList = "read_date,status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RainReading {

    /** 有效 */
    public static final int STATUS_VALID = 1;
    /** 作废 */
    public static final int STATUS_VOID = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 收集桶 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bucket_id", nullable = false)
    private RainBucket bucket;

    /** 自然日（哪一天接的水） */
    @Column(name = "read_date", nullable = false)
    private LocalDate readDate;

    /** 本次毫米数（>= 0，0.1 毫米精度；0 表示当日无降水） */
    @Column(name = "millimeters", nullable = false)
    private Double millimeters;

    /** 状态：1=有效，2=作废 */
    @Column(name = "status", nullable = false)
    private Integer status = STATUS_VALID;

    /** 作废原因（作废时必填，永久留痕） */
    @Column(name = "void_reason", length = 200)
    private String voidReason;

    /** 作废时间 */
    @Column(name = "void_time")
    private LocalDateTime voidTime;

    /** 版本号：记一笔时为 1，每次改数/作废 +1。改数或作废必须带上看到的版本，对不上即被他人抢先 */
    @Column(name = "version", nullable = false)
    private Integer version = 1;

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
