package com.example.geological.repository;

import com.example.geological.entity.TeamMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /** 某小队在职（status=1）队员数；停用队员不计入。 */
    @Query("SELECT COUNT(m) FROM TeamMember m WHERE m.team.id = :teamId AND m.status = 1")
    long countActiveByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT m FROM TeamMember m WHERE m.memberNo = :memberNo AND m.status = 1")
    Optional<TeamMember> findActiveByMemberNo(@Param("memberNo") String memberNo);

    @Query("SELECT m FROM TeamMember m JOIN FETCH m.team WHERE m.memberNo = :memberNo AND m.status = 1")
    Optional<TeamMember> findActiveWithTeamByMemberNo(@Param("memberNo") String memberNo);

    /**
     * 悲观写锁读取队员行。改挂/编辑落账前必须先拿到这把锁，
     * 把两人几乎同时把同一名停用队员改挂到不同小队的请求串行化：
     * 后到者在锁内复查 status，看到已离队即失败，不会留下被两队同时认领的记录。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM TeamMember m WHERE m.id = :id")
    Optional<TeamMember> findByIdForUpdate(@Param("id") Long id);
}
