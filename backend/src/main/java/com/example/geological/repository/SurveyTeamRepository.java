package com.example.geological.repository;

import com.example.geological.entity.SurveyTeam;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyTeamRepository extends JpaRepository<SurveyTeam, Long> {

    Optional<SurveyTeam> findByTeamCode(String teamCode);

    Optional<SurveyTeam> findByTeamName(String teamName);

    List<SurveyTeam> findByStatus(Integer status);

    /**
     * 悲观写锁读取小队行，用于「校验承重上限 + 落账」的串行化，
     * 防止并发挂靠把已用承重新增到超过上限。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM SurveyTeam t WHERE t.id = :id")
    Optional<SurveyTeam> findByIdForUpdate(@Param("id") Long id);
}