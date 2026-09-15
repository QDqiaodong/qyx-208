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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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

    /**
     * 悲观写锁锁定小队行。所有会改变小队已用承重的写路径都必须先拿到这把锁，
     * 再校验上限、落账，保证并发挂靠时「校验 + 落账」串行执行。
     */
    private SurveyTeam lockTeam(Long teamId) {
        return surveyTeamRepository.findByIdForUpdate(teamId)
                .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + teamId));
    }

    private double usedLoadOfTeam(Long teamId) {
        Double used = workstationRepository.sumLoadCapacityByTeamId(teamId);
        return used != null ? used : 0.0;
    }

    /**
     * 校验把承重为 newLoad 的操作台挂到该小队后是否超出随队总承重上限。
     * 只统计在用(status=1)操作台；若该台当前已挂在该小队且在用，
     * 其原承重已计入合计，校验时先剔除再按新承重计入。
     * 超限即抛错，本次挂靠不落账，绝不做自动挪台或自动放宽上限。
     */
    private void assertWithinTeamLimit(SurveyTeam team, Workstation workstation, double newLoad) {
        Double limit = team.getMaxLoadCapacity();
        if (limit == null) {
            throw new IllegalArgumentException("小队[" + team.getTeamName() + "]未核定随队总承重上限，请先在小队档案中设置");
        }
        double used = usedLoadOfTeam(team.getId());
        if (workstation != null && workstation.getId() != null
                && Objects.equals(workstation.getStatus(), 1)
                && workstation.getCurrentTeam() != null
                && workstation.getCurrentTeam().getId().equals(team.getId())
                && workstation.getLoadCapacity() != null) {
            used -= workstation.getLoadCapacity();
        }
        double after = used + newLoad;
        if (after > limit) {
            throw new IllegalArgumentException(String.format(
                    "超过小队[%s]随队总承重上限：上限 %.2f kg，当前已用 %.2f kg，本台承重 %.2f kg，超出 %.2f kg，本次挂靠未落账",
                    team.getTeamName(), limit, used, newLoad, after - limit));
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Workstation create(WorkstationDTO dto) {
        workstationRepository.findByWorkstationNo(dto.getWorkstationNo())
                .ifPresent(w -> {
                    throw new IllegalArgumentException("操作台编号已存在: " + dto.getWorkstationNo());
                });

        Workstation workstation = new Workstation();
        workstation.setWorkstationNo(dto.getWorkstationNo());
        double loadCapacity = parseLoadCapacity(dto.getLoadCapacity());
        workstation.setLoadCapacity(loadCapacity);
        workstation.setWorkstationType(dto.getWorkstationType());
        workstation.setAdaptStation(dto.getAdaptStation());

        if (dto.getCurrentTeamId() != null) {
            SurveyTeam team = lockTeam(dto.getCurrentTeamId());
            assertWithinTeamLimit(team, null, loadCapacity);
            workstation.setCurrentTeam(team);
        }

        Workstation saved = workstationRepository.save(workstation);
        cacheLoadCapacity(saved.getWorkstationNo(), saved.getLoadCapacity());
        return saved;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        double newLoad = parseLoadCapacity(dto.getLoadCapacity());

        Long currentTeamId = workstation.getCurrentTeam() != null ? workstation.getCurrentTeam().getId() : null;
        boolean teamChanged = !Objects.equals(currentTeamId, dto.getCurrentTeamId());
        if (!Objects.equals(workstation.getStatus(), 1) && teamChanged) {
            throw new IllegalArgumentException("操作台已停用，仍记在原小队名下，需先恢复在用才能改挂小队");
        }

        workstation.setLoadCapacity(newLoad);
        workstation.setWorkstationType(dto.getWorkstationType());
        workstation.setAdaptStation(dto.getAdaptStation());

        if (dto.getCurrentTeamId() != null) {
            SurveyTeam team = lockTeam(dto.getCurrentTeamId());
            if (Objects.equals(workstation.getStatus(), 1)) {
                assertWithinTeamLimit(team, workstation, newLoad);
            }
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Workstation restore(Long id) {
        Workstation workstation = workstationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("操作台不存在: " + id));
        if (Objects.equals(workstation.getStatus(), 1)) {
            throw new IllegalArgumentException("操作台当前为在用状态，无需恢复");
        }
        // 恢复在用后该台承重重新计入原小队已用承重，需对照上限校验
        if (workstation.getCurrentTeam() != null) {
            SurveyTeam team = lockTeam(workstation.getCurrentTeam().getId());
            assertWithinTeamLimit(team, null, workstation.getLoadCapacity());
            workstation.setCurrentTeam(team);
        }
        workstation.setStatus(1);
        Workstation saved = workstationRepository.save(workstation);
        cacheLoadCapacity(saved.getWorkstationNo(), saved.getLoadCapacity());
        return saved;
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
    public List<Workstation> findInactive() {
        return workstationRepository.findByStatus(0);
    }

    @Override
    public List<Workstation> findByTeamId(Long teamId) {
        return workstationRepository.findActiveByTeamId(teamId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void transfer(TransferDTO dto) {
        Workstation workstation = workstationRepository.findById(dto.getWorkstationId())
                .orElseThrow(() -> new IllegalArgumentException("操作台不存在: " + dto.getWorkstationId()));

        if (!Objects.equals(workstation.getStatus(), 1)) {
            throw new IllegalArgumentException("操作台已停用，仍记在原小队名下，需先恢复在用才能改挂到其他小队");
        }

        SurveyTeam toTeam = lockTeam(dto.getToTeamId());
        assertWithinTeamLimit(toTeam, workstation, workstation.getLoadCapacity());

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
