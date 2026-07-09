package com.example.geological.controller;

import com.example.geological.dto.ResponseDTO;
import com.example.geological.dto.SurveyTeamDTO;
import com.example.geological.dto.TeamAssetOverviewDTO;
import com.example.geological.entity.SurveyTeam;
import com.example.geological.service.SurveyTeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class SurveyTeamController {

    private final SurveyTeamService surveyTeamService;

    @GetMapping
    public ResponseDTO<List<SurveyTeamDTO>> getAll() {
        List<SurveyTeam> teams = surveyTeamService.findAll();
        List<SurveyTeamDTO> dtos = teams.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    @GetMapping("/{id}")
    public ResponseDTO<SurveyTeamDTO> getById(@PathVariable Long id) {
        SurveyTeam team = surveyTeamService.findById(id);
        return ResponseDTO.success(convertToDTO(team));
    }

    @GetMapping("/by-code/{teamCode}")
    public ResponseDTO<SurveyTeamDTO> getByCode(@PathVariable String teamCode) {
        SurveyTeam team = surveyTeamService.findByCode(teamCode);
        return ResponseDTO.success(convertToDTO(team));
    }

    @PostMapping
    public ResponseDTO<SurveyTeamDTO> create(@Valid @RequestBody SurveyTeamDTO dto) {
        SurveyTeam team = surveyTeamService.create(dto);
        return ResponseDTO.success("创建成功", convertToDTO(team));
    }

    @PutMapping("/{id}")
    public ResponseDTO<SurveyTeamDTO> update(@PathVariable Long id, @Valid @RequestBody SurveyTeamDTO dto) {
        SurveyTeam team = surveyTeamService.update(id, dto);
        return ResponseDTO.success("更新成功", convertToDTO(team));
    }

    @DeleteMapping("/{id}")
    public ResponseDTO<Void> delete(@PathVariable Long id) {
        surveyTeamService.delete(id);
        return ResponseDTO.success(null);
    }

    @GetMapping("/{id}/asset-overview")
    public ResponseDTO<TeamAssetOverviewDTO> getTeamAssetOverview(@PathVariable Long id) {
        TeamAssetOverviewDTO overview = surveyTeamService.getTeamAssetOverview(id);
        return ResponseDTO.success(overview);
    }

    @GetMapping("/asset-overview")
    public ResponseDTO<List<TeamAssetOverviewDTO>> getAllTeamAssetOverview() {
        List<TeamAssetOverviewDTO> overviews = surveyTeamService.getAllTeamAssetOverview();
        return ResponseDTO.success(overviews);
    }

    private SurveyTeamDTO convertToDTO(SurveyTeam team) {
        SurveyTeamDTO dto = new SurveyTeamDTO();
        dto.setId(team.getId());
        dto.setTeamName(team.getTeamName());
        dto.setTeamCode(team.getTeamCode());
        dto.setLeaderName(team.getLeaderName());
        dto.setLeaderPhone(team.getLeaderPhone());
        dto.setDescription(team.getDescription());
        return dto;
    }
}