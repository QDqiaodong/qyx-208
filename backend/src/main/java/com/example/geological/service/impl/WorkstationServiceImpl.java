package com.example.geological.service.impl;

import com.example.geological.dto.TransferDTO;
import com.example.geological.dto.WorkstationDTO;
import com.example.geological.entity.SurveyTeam;
import com.example.geological.entity.Workstation;
import com.example.geological.entity.WorkstationTransfer;
import com.example.geological.repository.SurveyTeamRepository;
import com.example.geological.repository.WorkstationRepository;
import com.example.geological.repository.WorkstationTransferRepository;
import com.example.geological.service.TransferNotificationService;
import com.example.geological.service.WorkstationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final TransferNotificationService transferNotificationService;
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
     * 适配工作站名字归一化：去掉首尾空白；空白串按「未填站名」处理（落 NULL，即释放占用）。
     */
    private String normalizeAdaptStation(String adaptStation) {
        if (adaptStation == null) {
            return null;
        }
        String trimmed = adaptStation.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 占用预检：在用操作台要占用某站名时，先看该站是否已被另一台在用操作台占着。
     * 停用台/空站名不占站，不参与校验；excludeId 用于编辑、恢复时排除自身。
     * 预检给出明确报错；两台几乎同时保存时由条件唯一索引做最终裁决（见 saveWithStationGuard）。
     */
    private void assertStationNotOccupied(String adaptStation, Long excludeId) {
        if (adaptStation == null) {
            return;
        }
        Long selfId = excludeId != null ? excludeId : -1L;
        workstationRepository.findActiveOccupant(adaptStation, selfId)
                .ifPresent(occupant -> {
                    throw new IllegalArgumentException(String.format(
                            "适配工作站[%s]已被在用操作台[%s]占用，同一时刻只允许一台在用操作台占用该站，本次保存失败，原占用保持不变",
                            adaptStation, occupant.getWorkstationNo()));
                });
    }

    /**
     * 保存并立即 flush，使条件唯一索引 uk_workstation_active_adapt_station 的冲突
     * 在本事务内抛出。两人几乎同时把两台在用台改成同一站名时，先提交的成功，
     * 后到的在此撞上唯一索引：翻译成明确业务错误，整笔事务回滚，绝不让两台同时占同一站。
     */
    private Workstation saveWithStationGuard(Workstation workstation) {
        try {
            return workstationRepository.saveAndFlush(workstation);
        } catch (DataIntegrityViolationException e) {
            if (isActiveAdaptStationConflict(e)) {
                throw new IllegalArgumentException(
                        "该适配工作站名字刚被另一台在用操作台占用（并发提交），本次保存失败，原占用保持不变");
            }
            throw e;
        }
    }

    private boolean isActiveAdaptStationConflict(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.contains("uk_workstation_active_adapt_station")) {
                return true;
            }
        }
        return false;
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
        String adaptStation = normalizeAdaptStation(dto.getAdaptStation());
        // 新建操作台默认在用：填了站名就要占用，先确认该站未被别的在用台占着
        assertStationNotOccupied(adaptStation, null);
        workstation.setAdaptStation(adaptStation);

        if (dto.getCurrentTeamId() != null) {
            SurveyTeam team = lockTeam(dto.getCurrentTeamId());
            assertWithinTeamLimit(team, null, loadCapacity);
            workstation.setCurrentTeam(team);
        }

        Workstation saved = saveWithStationGuard(workstation);
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
        String adaptStation = normalizeAdaptStation(dto.getAdaptStation());
        // 仅在用操作台占站：停用时的编辑不参与占用校验。
        // 站名未变（只改类型/承重/备注/所属小队）时预检按 id 排除自身，不会误伤；
        // 站名改成空等于释放本台对原站的占用。
        if (Objects.equals(workstation.getStatus(), 1)) {
            assertStationNotOccupied(adaptStation, workstation.getId());
        }
        workstation.setAdaptStation(adaptStation);

        if (dto.getCurrentTeamId() != null) {
            SurveyTeam team = lockTeam(dto.getCurrentTeamId());
            if (Objects.equals(workstation.getStatus(), 1)) {
                assertWithinTeamLimit(team, workstation, newLoad);
            }
            workstation.setCurrentTeam(team);
        } else {
            workstation.setCurrentTeam(null);
        }

        Workstation saved = saveWithStationGuard(workstation);
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
        String adaptStation = normalizeAdaptStation(workstation.getAdaptStation());
        workstation.setAdaptStation(adaptStation);
        // 停用期间该站可能已被别的在用台占用：恢复即重新占站，冲突时恢复失败，保持停用
        assertStationNotOccupied(adaptStation, workstation.getId());
        workstation.setStatus(1);
        Workstation saved = saveWithStationGuard(workstation);
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
        Workstation workstation = workstationRepository.findByIdForUpdate(dto.getWorkstationId())
                .orElseThrow(() -> new IllegalArgumentException("操作台不存在: " + dto.getWorkstationId()));

        if (!Objects.equals(workstation.getStatus(), 1)) {
            throw new IllegalArgumentException("操作台已停用，仍记在原小队名下，需先恢复在用才能改挂到其他小队");
        }

        SurveyTeam fromTeam = workstation.getCurrentTeam();
        if (fromTeam == null) {
            throw new IllegalArgumentException("操作台尚未归属小队，不能办理转队");
        }
        if (Objects.equals(fromTeam.getId(), dto.getToTeamId())) {
            throw new IllegalArgumentException("目标小队与原小队相同，无需转队");
        }

        SurveyTeam toTeam = lockTeam(dto.getToTeamId());
        assertWithinTeamLimit(toTeam, workstation, workstation.getLoadCapacity());

        WorkstationTransfer transfer = new WorkstationTransfer();
        transfer.setWorkstation(workstation);
        transfer.setFromTeam(fromTeam);
        transfer.setToTeam(toTeam);
        transfer.setTransferReason(dto.getTransferReason());
        transfer.setOperator(dto.getOperator());

        workstation.setCurrentTeam(toTeam);
        workstationRepository.save(workstation);
        WorkstationTransfer savedTransfer = transferRepository.save(transfer);

        // 通知与转队落账在同一个事务里：只有停用、超限等校验全部通过后才会到这里；
        // 通知落库失败也会整体回滚，不会留下转队成功但通知缺失的半笔记录。
        transferNotificationService.createTransferNotifications(savedTransfer);

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
