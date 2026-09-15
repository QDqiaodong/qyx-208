package com.example.geological.repository;

import com.example.geological.entity.SampleBag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SampleBagRepository extends JpaRepository<SampleBag, Long> {

    /**
     * 台账唯一性判定：同一小队、同一送检日、同一袋号。
     * 与数据库唯一索引 uk_sample_bag_team_date_bagno 对应，
     * 应用层先查、唯一索引兜底并发，双保险保证不会落两条。
     */
    Optional<SampleBag> findByTeamIdAndSubmitDateAndBagNo(Long teamId, LocalDate submitDate, String bagNo);

    List<SampleBag> findByTeamIdOrderBySubmitDateDescIdDesc(Long teamId);

    List<SampleBag> findByMemberIdOrderBySubmitDateDescIdDesc(Long memberId);

    List<SampleBag> findByStatusOrderBySubmitDateDescIdDesc(Integer status);

    @Query("SELECT b FROM SampleBag b JOIN FETCH b.team JOIN FETCH b.member ORDER BY b.submitDate DESC, b.id DESC")
    List<SampleBag> findAllWithDetails();

    @Query("SELECT b FROM SampleBag b JOIN FETCH b.team JOIN FETCH b.member WHERE b.team.id = :teamId ORDER BY b.submitDate DESC, b.id DESC")
    List<SampleBag> findWithDetailsByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT b FROM SampleBag b JOIN FETCH b.team JOIN FETCH b.member WHERE b.member.id = :memberId ORDER BY b.submitDate DESC, b.id DESC")
    List<SampleBag> findWithDetailsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT b FROM SampleBag b JOIN FETCH b.team JOIN FETCH b.member WHERE b.status = :status ORDER BY b.submitDate DESC, b.id DESC")
    List<SampleBag> findWithDetailsByStatus(@Param("status") Integer status);

    /** 在途袋数（status=1） */
    @Query("SELECT COUNT(b) FROM SampleBag b WHERE b.team.id = :teamId AND b.status = 1")
    long countInTransitByTeamId(@Param("teamId") Long teamId);

    /** 在途袋重合计(kg)（status=1） */
    @Query("SELECT COALESCE(SUM(b.bagWeight), 0) FROM SampleBag b WHERE b.team.id = :teamId AND b.status = 1")
    Double sumInTransitWeightByTeamId(@Param("teamId") Long teamId);
}
