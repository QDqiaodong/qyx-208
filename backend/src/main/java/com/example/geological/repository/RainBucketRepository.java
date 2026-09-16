package com.example.geological.repository;

import com.example.geological.entity.RainBucket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RainBucketRepository extends JpaRepository<RainBucket, Long> {

    Optional<RainBucket> findByBucketNo(String bucketNo);

    List<RainBucket> findAllByOrderByIdAsc();

    /**
     * 悲观写锁锁定收集桶行。同一桶的记一笔/改数/作废都必须先拿到这把锁，
     * 再在锁内查重、核对版本、落账并重算气象卡片合计。
     * 两名值班几乎同时动同一桶同一自然日时在此排队串行：
     * 先到的提交后版本已 +1，后到的锁内复查版本对不上即失败，只成一笔。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM RainBucket b WHERE b.id = :id")
    Optional<RainBucket> findByIdForUpdate(@Param("id") Long id);
}
