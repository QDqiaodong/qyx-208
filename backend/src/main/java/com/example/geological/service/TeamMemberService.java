package com.example.geological.service;

import com.example.geological.dto.TeamMemberDTO;
import com.example.geological.entity.TeamMember;

import java.util.List;

public interface TeamMemberService {

    TeamMember create(TeamMemberDTO dto);

    TeamMember update(Long id, TeamMemberDTO dto);

    void delete(Long id);

    TeamMember findById(Long id);

    TeamMember findByMemberNo(String memberNo);

    List<TeamMember> findByTeamId(Long teamId);

    List<TeamMember> findAll();

    List<TeamMember> findActiveByTeamId(Long teamId);
}