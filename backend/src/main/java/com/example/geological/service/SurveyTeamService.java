package com.example.geological.service;

import com.example.geological.dto.SurveyTeamDTO;
import com.example.geological.dto.TeamAssetOverviewDTO;
import com.example.geological.entity.SurveyTeam;

import java.util.List;

public interface SurveyTeamService {

    SurveyTeam create(SurveyTeamDTO dto);

    SurveyTeam update(Long id, SurveyTeamDTO dto);

    void delete(Long id);

    SurveyTeam findById(Long id);

    SurveyTeam findByCode(String teamCode);

    List<SurveyTeam> findAll();

    TeamAssetOverviewDTO getTeamAssetOverview(Long teamId);

    List<TeamAssetOverviewDTO> getAllTeamAssetOverview();
}