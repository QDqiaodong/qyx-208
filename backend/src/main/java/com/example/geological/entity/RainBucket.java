package com.example.geological.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 雨水收集桶档案（屋顶槽子接下来水用的收集桶）。
 *
 * 收集桶独立建档，只记桶号和备注；毫米数不落在桶上，
 * 每一笔读数（哪个桶、哪个自然日、多少毫米）落在 {@link RainReading}，
 * 气象卡片的当日合计落在 {@link RainDailyTotal}。
 */
@Entity
@Table(name = "rain_bucket")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RainBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 桶号（桶上贴的编号，全营唯一） */
    @Column(name = "bucket_no", nullable = false, unique = true, length = 50)
    private String bucketNo;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
