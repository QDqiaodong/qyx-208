package com.example.geological.controller;

import com.example.geological.dto.ResponseDTO;
import com.example.geological.dto.TeamAssetOverviewDTO;
import com.example.geological.dto.WorkstationDTO;
import com.example.geological.entity.TeamMember;
import com.example.geological.entity.Workstation;
import com.example.geological.service.SurveyTeamService;
import com.example.geological.service.TeamMemberService;
import com.example.geological.service.WorkstationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final TeamMemberService memberService;
    private final WorkstationService workstationService;
    private final SurveyTeamService teamService;

    @GetMapping("/team-asset-overview")
    public ResponseDTO<List<TeamAssetOverviewDTO>> getAllTeamAssetOverview() {
        return ResponseDTO.success(teamService.getAllTeamAssetOverview());
    }

    @GetMapping("/member/{memberNo}/workstations")
    public ResponseDTO<List<WorkstationDTO>> getWorkstationsByMemberNo(@PathVariable String memberNo) {
        TeamMember member = memberService.findByMemberNo(memberNo);
        if (member.getTeam() == null) {
            return ResponseDTO.success(List.of());
        }
        List<Workstation> workstations = workstationService.findByTeamId(member.getTeam().getId());
        List<WorkstationDTO> dtos = workstations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    @GetMapping("/member/{memberNo}/team-asset-overview")
    public ResponseDTO<TeamAssetOverviewDTO> getTeamAssetOverviewByMemberNo(@PathVariable String memberNo) {
        TeamMember member = memberService.findByMemberNo(memberNo);
        if (member.getTeam() == null) {
            return ResponseDTO.success(null);
        }
        return ResponseDTO.success(teamService.getTeamAssetOverview(member.getTeam().getId()));
    }

    private WorkstationDTO convertToDTO(Workstation workstation) {
        WorkstationDTO dto = new WorkstationDTO();
        dto.setId(workstation.getId());
        dto.setWorkstationNo(workstation.getWorkstationNo());
        dto.setLoadCapacity(workstation.getLoadCapacity() != null ? workstation.getLoadCapacity().toString() : null);
        dto.setWorkstationType(workstation.getWorkstationType());
        dto.setAdaptStation(workstation.getAdaptStation());
        dto.setStatus(workstation.getStatus());
        if (workstation.getCurrentTeam() != null) {
            dto.setCurrentTeamId(workstation.getCurrentTeam().getId());
            dto.setCurrentTeamName(workstation.getCurrentTeam().getTeamName());
        }
        return dto;
    }
}
