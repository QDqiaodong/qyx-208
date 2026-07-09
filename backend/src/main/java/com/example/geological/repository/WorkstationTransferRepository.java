package com.example.geological.repository;

import com.example.geological.entity.WorkstationTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkstationTransferRepository extends JpaRepository<WorkstationTransfer, Long> {

    List<WorkstationTransfer> findByWorkstationIdOrderByTransferTimeDesc(Long workstationId);

    @Query("SELECT t FROM WorkstationTransfer t JOIN FETCH t.workstation JOIN FETCH t.fromTeam JOIN FETCH t.toTeam WHERE t.workstation.id = :workstationId ORDER BY t.transferTime DESC")
    List<WorkstationTransfer> findByWorkstationIdWithDetails(@Param("workstationId") Long workstationId);
}