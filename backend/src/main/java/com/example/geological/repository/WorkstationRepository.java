package com.example.geological.repository;

import com.example.geological.entity.Workstation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkstationRepository extends JpaRepository<Workstation, Long> {

    Optional<Workstation> findByWorkstationNo(String workstationNo);

    List<Workstation> findByCurrentTeamId(Long teamId);

    List<Workstation> findByStatus(Integer status);

    @Query("SELECT w FROM Workstation w WHERE w.currentTeam.id = :teamId AND w.status = 1")
    List<Workstation> findActiveByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT SUM(w.loadCapacity) FROM Workstation w WHERE w.currentTeam.id = :teamId AND w.status = 1")
    Double sumLoadCapacityByTeamId(@Param("teamId") Long teamId);
}