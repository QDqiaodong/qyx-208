package com.example.geological.service.impl;

import com.example.geological.dto.TeamMemberDTO;
import com.example.geological.entity.SurveyTeam;
import com.example.geological.entity.TeamMember;
import com.example.geological.repository.SurveyTeamRepository;
import com.example.geological.repository.TeamMemberRepository;
import com.example.geological.service.TeamMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamMemberServiceImpl implements TeamMemberService {

    private final TeamMemberRepository memberRepository;
    private final SurveyTeamRepository surveyTeamRepository;

    @Override
    @Transactional
    public TeamMember create(TeamMemberDTO dto) {
        memberRepository.findByMemberNo(dto.getMemberNo())
                .ifPresent(m -> {
                    throw new IllegalArgumentException("队员编号已存在: " + dto.getMemberNo());
                });

        SurveyTeam team = surveyTeamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + dto.getTeamId()));

        TeamMember member = new TeamMember();
        member.setMemberNo(dto.getMemberNo());
        member.setMemberName(dto.getMemberName());
        member.setPhone(dto.getPhone());
        member.setPosition(dto.getPosition());
        member.setTeam(team);

        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public TeamMember update(Long id, TeamMemberDTO dto) {
        TeamMember member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("队员不存在: " + id));

        if (!member.getMemberNo().equals(dto.getMemberNo())) {
            memberRepository.findByMemberNo(dto.getMemberNo())
                    .ifPresent(m -> {
                        throw new IllegalArgumentException("队员编号已存在: " + dto.getMemberNo());
                    });
            member.setMemberNo(dto.getMemberNo());
        }

        member.setMemberName(dto.getMemberName());
        member.setPhone(dto.getPhone());
        member.setPosition(dto.getPosition());

        if (dto.getTeamId() != null && !dto.getTeamId().equals(member.getTeam().getId())) {
            SurveyTeam team = surveyTeamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + dto.getTeamId()));
            member.setTeam(team);
        }

        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TeamMember member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("队员不存在: " + id));
        member.setStatus(0);
        memberRepository.save(member);
    }

    @Override
    public TeamMember findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("队员不存在: " + id));
    }

    @Override
    public TeamMember findByMemberNo(String memberNo) {
        return memberRepository.findActiveWithTeamByMemberNo(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("队员不存在: " + memberNo));
    }

    @Override
    public List<TeamMember> findByTeamId(Long teamId) {
        return memberRepository.findByTeamId(teamId);
    }

    @Override
    public List<TeamMember> findAll() {
        return memberRepository.findAll().stream()
                .filter(m -> m.getStatus() == 1)
                .toList();
    }

    @Override
    public List<TeamMember> findActiveByTeamId(Long teamId) {
        return memberRepository.findByTeamIdAndStatus(teamId, 1);
    }
}