package com.example.geological.service.impl;

import com.example.geological.dto.SurveyTeamDTO;
import com.example.geological.dto.TeamAssetOverviewDTO;
import com.example.geological.dto.WorkstationDTO;
import com.example.geological.entity.SurveyTeam;
import com.example.geological.entity.Workstation;
import com.example.geological.repository.SampleBagRepository;
import com.example.geological.repository.SurveyTeamRepository;
import com.example.geological.repository.TeamMemberRepository;
import com.example.geological.repository.WorkstationRepository;
import com.example.geological.service.SurveyTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyTeamServiceImpl implements SurveyTeamService {

    private final SurveyTeamRepository surveyTeamRepository;
    private final WorkstationRepository workstationRepository;
    private final SampleBagRepository sampleBagRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    @Transactional
    public SurveyTeam create(SurveyTeamDTO dto) {
        surveyTeamRepository.findByTeamCode(dto.getTeamCode())
                .ifPresent(t -> {
                    throw new IllegalArgumentException("小队编码已存在: " + dto.getTeamCode());
                });

        surveyTeamRepository.findByTeamName(dto.getTeamName())
                .ifPresent(t -> {
                    throw new IllegalArgumentException("小队名称已存在: " + dto.getTeamName());
                });

        SurveyTeam team = new SurveyTeam();
        team.setTeamName(dto.getTeamName());
        team.setTeamCode(dto.getTeamCode());
        team.setLeaderName(dto.getLeaderName());
        team.setLeaderPhone(dto.getLeaderPhone());
        team.setDescription(dto.getDescription());
        team.setMaxLoadCapacity(dto.getMaxLoadCapacity());

        return surveyTeamRepository.save(team);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SurveyTeam update(Long id, SurveyTeamDTO dto) {
        SurveyTeam team = surveyTeamRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + id));

        if (!team.getTeamCode().equals(dto.getTeamCode())) {
            surveyTeamRepository.findByTeamCode(dto.getTeamCode())
                    .ifPresent(t -> {
                        throw new IllegalArgumentException("小队编码已存在: " + dto.getTeamCode());
                    });
            team.setTeamCode(dto.getTeamCode());
        }

        if (!team.getTeamName().equals(dto.getTeamName())) {
            surveyTeamRepository.findByTeamName(dto.getTeamName())
                    .ifPresent(t -> {
                        throw new IllegalArgumentException("小队名称已存在: " + dto.getTeamName());
                    });
            team.setTeamName(dto.getTeamName());
        }

        // 上限只能调低到不低于当前在用台承重合计，否则本次修改整体失败，
        // 不能留下账面已经超限的小队
        Double used = workstationRepository.sumLoadCapacityByTeamId(id);
        double usedLoad = used != null ? used : 0.0;
        if (dto.getMaxLoadCapacity() < usedLoad) {
            throw new IllegalArgumentException(String.format(
                    "随队总承重上限不能低于当前已用承重：小队[%s]当前已用 %.2f kg，新上限 %.2f kg，低于已用 %.2f kg，本次修改未落账",
                    team.getTeamName(), usedLoad, dto.getMaxLoadCapacity(), usedLoad - dto.getMaxLoadCapacity()));
        }
        team.setMaxLoadCapacity(dto.getMaxLoadCapacity());

        team.setLeaderName(dto.getLeaderName());
        team.setLeaderPhone(dto.getLeaderPhone());
        team.setDescription(dto.getDescription());

        return surveyTeamRepository.save(team);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SurveyTeam team = surveyTeamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + id));
        team.setStatus(0);
        surveyTeamRepository.save(team);
    }

    @Override
    public SurveyTeam findById(Long id) {
        return surveyTeamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + id));
    }

    @Override
    public SurveyTeam findByCode(String teamCode) {
        return surveyTeamRepository.findByTeamCode(teamCode)
                .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + teamCode));
    }

    @Override
    public List<SurveyTeam> findAll() {
        return surveyTeamRepository.findByStatus(1);
    }

    @Override
    public TeamAssetOverviewDTO getTeamAssetOverview(Long teamId) {
        SurveyTeam team = surveyTeamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + teamId));

        List<Workstation> workstations = workstationRepository.findActiveByTeamId(teamId);
        Double totalLoadCapacity = workstationRepository.sumLoadCapacityByTeamId(teamId);

        List<WorkstationDTO> workstationDTOs = workstations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        TeamAssetOverviewDTO overview = new TeamAssetOverviewDTO();
        overview.setTeamId(team.getId());
        overview.setTeamName(team.getTeamName());
        overview.setTeamCode(team.getTeamCode());
        // 在职人数只统计 status=1 的队员；停用离队者立即从小队档案人数中剔除
        overview.setActiveMemberCount(teamMemberRepository.countActiveByTeamId(team.getId()));
        overview.setWorkstationCount(workstations.size());
        double usedLoad = totalLoadCapacity != null ? totalLoadCapacity : 0.0;
        double maxLoad = team.getMaxLoadCapacity() != null ? team.getMaxLoadCapacity() : 0.0;
        overview.setMaxLoadCapacity(maxLoad);
        overview.setTotalLoadCapacity(usedLoad);
        overview.setRemainingLoadCapacity(maxLoad - usedLoad);
        overview.setWorkstations(workstationDTOs);

        // 在途样品袋：已登记送检、尚未出站办结。办结袋不计入在途袋数和在途袋重
        overview.setInTransitBagCount(sampleBagRepository.countInTransitByTeamId(teamId));
        overview.setInTransitBagWeight(sampleBagRepository.sumInTransitWeightByTeamId(teamId));

        return overview;
    }

    @Override
    public List<TeamAssetOverviewDTO> getAllTeamAssetOverview() {
        List<SurveyTeam> teams = surveyTeamRepository.findByStatus(1);
        return teams.stream()
                .map(team -> getTeamAssetOverview(team.getId()))
                .collect(Collectors.toList());
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