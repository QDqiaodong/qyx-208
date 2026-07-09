package com.example.geological.controller;

import com.example.geological.dto.ResponseDTO;
import com.example.geological.dto.TeamAssetOverviewDTO;
import com.example.geological.dto.WorkstationDTO;
import com.example.geological.entity.SurveyTeam;
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
        List<SurveyTeam> teams = teamService.findAll();
        List<TeamAssetOverviewDTO> overviews = teams.stream()
                .map(team -> {
                    List<Workstation> workstations = workstationService.findByTeamId(team.getId());
                    Double totalLoadCapacity = workstations.stream()
                            .filter(w -> w.getLoadCapacity() != null)
                            .mapToDouble(Workstation::getLoadCapacity)
                            .sum();
                    List<WorkstationDTO> workstationDTOs = workstations.stream()
                            .map(this::convertToDTO)
                            .collect(Collectors.toList());
                    TeamAssetOverviewDTO overview = new TeamAssetOverviewDTO();
                    overview.setTeamId(team.getId());
                    overview.setTeamName(team.getTeamName());
                    overview.setTeamCode(team.getTeamCode());
                    overview.setWorkstationCount(workstations.size());
                    overview.setTotalLoadCapacity(totalLoadCapacity);
                    overview.setWorkstations(workstationDTOs);
                    return overview;
                })
                .collect(Collectors.toList());
        return ResponseDTO.success(overviews);
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
        List<Workstation> workstations = workstationService.findByTeamId(member.getTeam().getId());
        Double totalLoadCapacity = workstations.stream()
                .mapToDouble(Workstation::getLoadCapacity)
                .sum();

        List<WorkstationDTO> workstationDTOs = workstations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        TeamAssetOverviewDTO overview = new TeamAssetOverviewDTO();
        overview.setTeamId(member.getTeam().getId());
        overview.setTeamName(member.getTeam().getTeamName());
        overview.setTeamCode(member.getTeam().getTeamCode());
        overview.setWorkstationCount(workstations.size());
        overview.setTotalLoadCapacity(totalLoadCapacity);
        overview.setWorkstations(workstationDTOs);

        return ResponseDTO.success(overview);
    }

    private WorkstationDTO convertToDTO(Workstation workstation) {
        WorkstationDTO dto = new WorkstationDTO();
        dto.setId(workstation.getId());
        dto.setWorkstationNo(workstation.getWorkstationNo());
        dto.setLoadCapacity(workstation.getLoadCapacity() != null ? workstation.getLoadCapacity().toString() : null);
        dto.setWorkstationType(workstation.getWorkstationType());
        dto.setAdaptStation(workstation.getAdaptStation());
        if (workstation.getCurrentTeam() != null) {
            dto.setCurrentTeamId(workstation.getCurrentTeam().getId());
            dto.setCurrentTeamName(workstation.getCurrentTeam().getTeamName());
        }
        return dto;
    }
}