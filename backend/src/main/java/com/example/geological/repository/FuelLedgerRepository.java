package com.example.geological.repository;

import com.example.geological.entity.FuelLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FuelLedgerRepository extends JpaRepository<FuelLedger, Long> {

    @Query("SELECT l FROM FuelLedger l JOIN FETCH l.barrel LEFT JOIN FETCH l.team LEFT JOIN FETCH l.member ORDER BY l.ledgerDate DESC, l.id DESC")
    List<FuelLedger> findAllWithDetails();

    @Query("SELECT l FROM FuelLedger l JOIN FETCH l.barrel LEFT JOIN FETCH l.team LEFT JOIN FETCH l.member WHERE l.barrel.id = :barrelId ORDER BY l.ledgerDate DESC, l.id DESC")
    List<FuelLedger> findWithDetailsByBarrelId(@Param("barrelId") Long barrelId);

    @Query("SELECT l FROM FuelLedger l JOIN FETCH l.barrel LEFT JOIN FETCH l.team LEFT JOIN FETCH l.member WHERE l.team.id = :teamId ORDER BY l.ledgerDate DESC, l.id DESC")
    List<FuelLedger> findWithDetailsByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT l FROM FuelLedger l JOIN FETCH l.barrel LEFT JOIN FETCH l.team LEFT JOIN FETCH l.member WHERE l.member.id = :memberId ORDER BY l.ledgerDate DESC, l.id DESC")
    List<FuelLedger> findWithDetailsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT l FROM FuelLedger l JOIN FETCH l.barrel LEFT JOIN FETCH l.team LEFT JOIN FETCH l.member WHERE l.type = :type ORDER BY l.ledgerDate DESC, l.id DESC")
    List<FuelLedger> findWithDetailsByType(@Param("type") Integer type);
}
