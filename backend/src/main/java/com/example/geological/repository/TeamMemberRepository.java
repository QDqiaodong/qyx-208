package com.example.geological.repository;

import com.example.geological.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByMemberNo(String memberNo);

    List<TeamMember> findByTeamId(Long teamId);

    List<TeamMember> findByTeamIdAndStatus(Long teamId, Integer status);

    @Query("SELECT m FROM TeamMember m WHERE m.memberNo = :memberNo AND m.status = 1")
    Optional<TeamMember> findActiveByMemberNo(@Param("memberNo") String memberNo);

    @Query("SELECT m FROM TeamMember m JOIN FETCH m.team WHERE m.memberNo = :memberNo AND m.status = 1")
    Optional<TeamMember> findActiveWithTeamByMemberNo(@Param("memberNo") String memberNo);
}