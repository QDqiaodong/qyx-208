package com.example.geological.service.impl;

import com.example.geological.dto.TransferDTO;
import com.example.geological.dto.WorkstationDTO;
import com.example.geological.entity.SurveyTeam;
import com.example.geological.entity.Workstation;
import com.example.geological.entity.WorkstationTransfer;
import com.example.geological.repository.SurveyTeamRepository;
import com.example.geological.repository.WorkstationRepository;
import com.example.geological.repository.WorkstationTransferRepository;
import com.example.geological.service.WorkstationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkstationServiceImpl implements WorkstationService {

    private final WorkstationRepository workstationRepository;
    private final SurveyTeamRepository surveyTeamRepository;
    private final WorkstationTransferRepository transferRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "workstation:loadcapacity:";

    private Double parseLoadCapacity(String loadCapacityStr) {
        if (loadCapacityStr == null || loadCapacityStr.trim().isEmpty()) {
            throw new IllegalArgumentException("承重不能为空");
        }
        String cleaned = loadCapacityStr.replaceAll("[^0-9.]", "");
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("承重格式不正确: " + loadCapacityStr);
        }
    }

    @Override
    @Transactional
    public Workstation create(WorkstationDTO dto) {
        workstationRepository.findByWorkstationNo(dto.getWorkstationNo())
                .ifPresent(w -> {
                    throw new IllegalArgumentException("操作台编号已存在: " + dto.getWorkstationNo());
                });

        Workstation workstation = new Workstation();
        workstation.setWorkstationNo(dto.getWorkstationNo());
        workstation.setLoadCapacity(parseLoadCapacity(dto.getLoadCapacity()));
        workstation.setWorkstationType(dto.getWorkstationType());
        workstation.setAdaptStation(dto.getAdaptStation());

        if (dto.getCurrentTeamId() != null) {
            SurveyTeam team = surveyTeamRepository.findById(dto.getCurrentTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + dto.getCurrentTeamId()));
            workstation.setCurrentTeam(team);
        }

        Workstation saved = workstationRepository.save(workstation);
        cacheLoadCapacity(saved.getWorkstationNo(), saved.getLoadCapacity());
        return saved;
    }

    @Override
    @Transactional
    public Workstation update(Long id, WorkstationDTO dto) {
        Workstation workstation = workstationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("操作台不存在: " + id));

        if (!workstation.getWorkstationNo().equals(dto.getWorkstationNo())) {
            workstationRepository.findByWorkstationNo(dto.getWorkstationNo())
                    .ifPresent(w -> {
                        throw new IllegalArgumentException("操作台编号已存在: " + dto.getWorkstationNo());
                    });
            workstation.setWorkstationNo(dto.getWorkstationNo());
        }

        workstation.setLoadCapacity(parseLoadCapacity(dto.getLoadCapacity()));
        workstation.setWorkstationType(dto.getWorkstationType());
        workstation.setAdaptStation(dto.getAdaptStation());

        if (dto.getCurrentTeamId() != null) {
            SurveyTeam team = surveyTeamRepository.findById(dto.getCurrentTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + dto.getCurrentTeamId()));
            workstation.setCurrentTeam(team);
        } else {
            workstation.setCurrentTeam(null);
        }

        Workstation saved = workstationRepository.save(workstation);
        cacheLoadCapacity(saved.getWorkstationNo(), saved.getLoadCapacity());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Workstation workstation = workstationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("操作台不存在: " + id));
        workstation.setStatus(0);
        workstationRepository.save(workstation);
        redisTemplate.delete(CACHE_KEY_PREFIX + workstation.getWorkstationNo());
    }

    @Override
    public Workstation findById(Long id) {
        return workstationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("操作台不存在: " + id));
    }

    @Override
    public Workstation findByNo(String workstationNo) {
        return workstationRepository.findByWorkstationNo(workstationNo)
                .orElseThrow(() -> new IllegalArgumentException("操作台不存在: " + workstationNo));
    }

    @Override
    public List<Workstation> findAll() {
        return workstationRepository.findByStatus(1);
    }

    @Override
    public List<Workstation> findByTeamId(Long teamId) {
        return workstationRepository.findActiveByTeamId(teamId);
    }

    @Override
    @Transactional
    public void transfer(TransferDTO dto) {
        Workstation workstation = workstationRepository.findById(dto.getWorkstationId())
                .orElseThrow(() -> new IllegalArgumentException("操作台不存在: " + dto.getWorkstationId()));

        SurveyTeam toTeam = surveyTeamRepository.findById(dto.getToTeamId())
                .orElseThrow(() -> new IllegalArgumentException("目标小队不存在: " + dto.getToTeamId()));

        SurveyTeam fromTeam = workstation.getCurrentTeam();

        WorkstationTransfer transfer = new WorkstationTransfer();
        transfer.setWorkstation(workstation);
        transfer.setFromTeam(fromTeam);
        transfer.setToTeam(toTeam);
        transfer.setTransferReason(dto.getTransferReason());
        transfer.setOperator(dto.getOperator());

        workstation.setCurrentTeam(toTeam);
        workstationRepository.save(workstation);
        transferRepository.save(transfer);

        cacheLoadCapacity(workstation.getWorkstationNo(), workstation.getLoadCapacity());
    }

    @Override
    public List<WorkstationTransfer> getTransferHistory(Long workstationId) {
        return transferRepository.findByWorkstationIdWithDetails(workstationId);
    }

    @Override
    public Double getLoadCapacityFromCache(String workstationNo) {
        String key = CACHE_KEY_PREFIX + workstationNo;
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Double.parseDouble(value.toString());
        }
        Optional<Workstation> workstation = workstationRepository.findByWorkstationNo(workstationNo);
        if (workstation.isPresent()) {
            Double loadCapacity = workstation.get().getLoadCapacity();
            cacheLoadCapacity(workstationNo, loadCapacity);
            return loadCapacity;
        }
        return null;
    }

    @Override
    public void cacheLoadCapacity(String workstationNo, Double loadCapacity) {
        String key = CACHE_KEY_PREFIX + workstationNo;
        redisTemplate.opsForValue().set(key, loadCapacity.toString());
    }
}