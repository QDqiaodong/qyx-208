package com.example.geological.controller;

import com.example.geological.dto.ResponseDTO;
import com.example.geological.dto.TeamMemberDTO;
import com.example.geological.entity.TeamMember;
import com.example.geological.service.TeamMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService memberService;

    @GetMapping
    public ResponseDTO<List<TeamMemberDTO>> getAll() {
        List<TeamMember> members = memberService.findAll();
        List<TeamMemberDTO> dtos = members.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    @GetMapping("/{id}")
    public ResponseDTO<TeamMemberDTO> getById(@PathVariable Long id) {
        TeamMember member = memberService.findById(id);
        return ResponseDTO.success(convertToDTO(member));
    }

    @GetMapping("/by-no/{memberNo}")
    public ResponseDTO<TeamMemberDTO> getByMemberNo(@PathVariable String memberNo) {
        TeamMember member = memberService.findByMemberNo(memberNo);
        return ResponseDTO.success(convertToDTO(member));
    }

    @GetMapping("/by-team/{teamId}")
    public ResponseDTO<List<TeamMemberDTO>> getByTeamId(@PathVariable Long teamId) {
        List<TeamMember> members = memberService.findActiveByTeamId(teamId);
        List<TeamMemberDTO> dtos = members.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    @PostMapping
    public ResponseDTO<TeamMemberDTO> create(@Valid @RequestBody TeamMemberDTO dto) {
        TeamMember member = memberService.create(dto);
        return ResponseDTO.success("创建成功", convertToDTO(member));
    }

    @PutMapping("/{id}")
    public ResponseDTO<TeamMemberDTO> update(@PathVariable Long id, @Valid @RequestBody TeamMemberDTO dto) {
        TeamMember member = memberService.update(id, dto);
        return ResponseDTO.success("更新成功", convertToDTO(member));
    }

    @DeleteMapping("/{id}")
    public ResponseDTO<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseDTO.success(null);
    }

    private TeamMemberDTO convertToDTO(TeamMember member) {
        TeamMemberDTO dto = new TeamMemberDTO();
        dto.setId(member.getId());
        dto.setMemberNo(member.getMemberNo());
        dto.setMemberName(member.getMemberName());
        dto.setPhone(member.getPhone());
        dto.setPosition(member.getPosition());
        if (member.getTeam() != null) {
            dto.setTeamId(member.getTeam().getId());
            dto.setTeamName(member.getTeam().getTeamName());
        }
        return dto;
    }
}