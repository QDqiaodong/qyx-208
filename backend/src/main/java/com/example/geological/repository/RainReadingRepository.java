package com.example.geological.repository;

import com.example.geological.entity.RainReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RainReadingRepository extends JpaRepository<RainReading, Long> {

    /** 查某桶某自然日指定状态的读数（记一笔前在桶锁内查重：同桶同日只留一条有效读数） */
    List<RainReading> findByBucket_IdAndReadDateAndStatus(Long bucketId, LocalDate readDate, Integer status);

    @Query("SELECT r FROM RainReading r JOIN FETCH r.bucket b ORDER BY r.readDate DESC, b.bucketNo ASC, r.id DESC")
    List<RainReading> findAllWithBucket();

    /** 某自然日各桶有效读数合计（气象卡片重算的唯一口径：只加有效读数，作废不计） */
    @Query("SELECT COALESCE(SUM(r.millimeters), 0.0) FROM RainReading r " +
            "WHERE r.readDate = :readDate AND r.status = 1")
    Double sumValidByReadDate(@Param("readDate") LocalDate readDate);

    /** 各自然日的有效读数条数（卡片上展示「按几笔有效读数加出来的」） */
    @Query("SELECT r.readDate, COUNT(r) FROM RainReading r WHERE r.status = 1 GROUP BY r.readDate")
    List<Object[]> countValidGroupByReadDate();
}
