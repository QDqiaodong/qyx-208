package com.example.geological.repository;

import com.example.geological.entity.Workstation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkstationRepository extends JpaRepository<Workstation, Long> {

    Optional<Workstation> findByWorkstationNo(String workstationNo);

    /**
     * 查询某一适配工作站名字当前被哪台在用操作台占用（status=1）。
     * 停用台与空站名不占站；excludeId 用于编辑/恢复时排除自身。
     */
    @Query("SELECT w FROM Workstation w WHERE w.adaptStation = :adaptStation AND w.status = 1 AND w.id <> :excludeId")
    Optional<Workstation> findActiveOccupant(@Param("adaptStation") String adaptStation,
                                             @Param("excludeId") Long excludeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Workstation w WHERE w.id = :id")
    Optional<Workstation> findByIdForUpdate(@Param("id") Long id);

    List<Workstation> findByCurrentTeamId(Long teamId);

    List<Workstation> findByStatus(Integer status);

    @Query("SELECT w FROM Workstation w WHERE w.currentTeam.id = :teamId AND w.status = 1")
    List<Workstation> findActiveByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT SUM(w.loadCapacity) FROM Workstation w WHERE w.currentTeam.id = :teamId AND w.status = 1")
    Double sumLoadCapacityByTeamId(@Param("teamId") Long teamId);
}