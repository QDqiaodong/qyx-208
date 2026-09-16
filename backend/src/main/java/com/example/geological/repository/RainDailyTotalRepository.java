package com.example.geological.repository;

import com.example.geological.entity.RainDailyTotal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RainDailyTotalRepository extends JpaRepository<RainDailyTotal, Long> {

    Optional<RainDailyTotal> findByReadDate(LocalDate readDate);

    /**
     * 悲观写锁锁定某自然日的卡片行。改不同桶但同一自然日的两笔写在此串行：
     * 后到者等先到者提交后再重算合计，看到的是先到者已落账的有效读数，
     * 合计不会丢掉任何一笔已提交的读数。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RainDailyTotal t WHERE t.readDate = :readDate")
    Optional<RainDailyTotal> findByReadDateForUpdate(@Param("readDate") LocalDate readDate);

    List<RainDailyTotal> findAllByOrderByReadDateDesc();

    List<RainDailyTotal> findByReadDateBetweenOrderByReadDateDesc(LocalDate from, LocalDate to);
}
