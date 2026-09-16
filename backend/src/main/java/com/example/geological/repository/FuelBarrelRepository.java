package com.example.geological.repository;

import com.example.geological.entity.FuelBarrel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuelBarrelRepository extends JpaRepository<FuelBarrel, Long> {

    Optional<FuelBarrel> findByBarrelNo(String barrelNo);

    List<FuelBarrel> findAllByOrderByIdAsc();

    /**
     * 悲观写锁锁定油桶行。所有会改变该桶余量的写路径（领用扣减、回灌增加）
     * 都必须先拿到这把锁，再在锁内复查余量并完成「余量校验 + 余量改写 + 台账落账」。
     * 两人几乎同时对同一桶各舀一笔时在此串行：先到的提交后余量已下降，
     * 后到的复查发现余量不足即整笔失败，余量绝不会被扣成负数。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM FuelBarrel b WHERE b.id = :id")
    Optional<FuelBarrel> findByIdForUpdate(@Param("id") Long id);
}
