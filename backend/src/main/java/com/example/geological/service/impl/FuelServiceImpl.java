package com.example.geological.service.impl;

import com.example.geological.dto.FuelBarrelDTO;
import com.example.geological.dto.FuelIssueDTO;
import com.example.geological.dto.FuelReturnDTO;
import com.example.geological.entity.FuelBarrel;
import com.example.geological.entity.FuelLedger;
import com.example.geological.entity.SurveyTeam;
import com.example.geological.entity.TeamMember;
import com.example.geological.repository.FuelBarrelRepository;
import com.example.geological.repository.FuelLedgerRepository;
import com.example.geological.repository.SurveyTeamRepository;
import com.example.geological.repository.TeamMemberRepository;
import com.example.geological.service.FuelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuelServiceImpl implements FuelService {

    private final FuelBarrelRepository barrelRepository;
    private final FuelLedgerRepository ledgerRepository;
    private final SurveyTeamRepository surveyTeamRepository;
    private final TeamMemberRepository memberRepository;

    /** 领用 */
    private static final int TYPE_ISSUE = 1;
    /** 回灌 */
    private static final int TYPE_RETURN = 2;

    /** 浮点余量比较容差，避免 0.1+0.2 这类误差把临界账误判 */
    private static final double EPS = 1e-9;

    private double normalize(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private LocalDate parseDate(String date, String fieldLabel) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldLabel + "格式不正确，应为 yyyy-MM-dd: " + date);
        }
    }

    private double requirePositive(Double liters, String fieldLabel) {
        if (liters == null || liters <= 0) {
            throw new IllegalArgumentException(fieldLabel + "必须大于0");
        }
        return normalize(liters);
    }

    private FuelBarrel lockBarrel(Long barrelId) {
        return barrelRepository.findByIdForUpdate(barrelId)
                .orElseThrow(() -> new IllegalArgumentException("油桶不存在: " + barrelId));
    }

    /**
     * 校验领油队员在职、且属于所登记的小队（停用离队队员不能领油）。
     */
    private TeamMember resolveMemberOfTeam(Long memberId, SurveyTeam team) {
        TeamMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("领油队员不存在: " + memberId));
        if (!Objects.equals(member.getStatus(), 1)) {
            throw new IllegalArgumentException("领油队员已停用离队，不能登记领油: " + member.getMemberNo());
        }
        if (member.getTeam() == null || !Objects.equals(member.getTeam().getId(), team.getId())) {
            throw new IllegalArgumentException(String.format(
                    "领油队员[%s]不属于小队[%s]，不能由该小队登记领油",
                    member.getMemberName(), team.getTeamName()));
        }
        return member;
    }

    @Override
    @Transactional
    public FuelBarrel createBarrel(FuelBarrelDTO dto) {
        String barrelNo = dto.getBarrelNo() == null ? "" : dto.getBarrelNo().trim();
        if (barrelNo.isEmpty()) {
            throw new IllegalArgumentException("桶号不能为空");
        }
        double capacity = requirePositive(dto.getRatedCapacity(), "额定升数");

        if (barrelRepository.findByBarrelNo(barrelNo).isPresent()) {
            throw new IllegalArgumentException("桶号已存在，不能重复建档: " + barrelNo);
        }

        FuelBarrel barrel = new FuelBarrel();
        barrel.setBarrelNo(barrelNo);
        barrel.setRatedCapacity(capacity);

        double initial;
        if (dto.getCurrentLevel() == null) {
            // 新桶默认满桶入库
            initial = capacity;
        } else {
            initial = normalize(dto.getCurrentLevel());
            if (initial < -EPS || initial > capacity + EPS) {
                throw new IllegalArgumentException(String.format(
                        "初始余量必须在 0 与额定升数(%.3f)之间", capacity));
            }
            if (initial < 0) {
                initial = 0;
            }
        }
        barrel.setCurrentLevel(initial);
        barrel.setRemark(dto.getRemark());
        return barrelRepository.save(barrel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuelBarrel> findAllBarrels() {
        return barrelRepository.findAllByOrderByIdAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public FuelBarrel findBarrelById(Long id) {
        return barrelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("油桶不存在: " + id));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FuelLedger issue(FuelIssueDTO dto) {
        double liters = requirePositive(dto.getLiters(), "领用升数");
        LocalDate date = parseDate(dto.getIssueDate(), "领用日期");

        // 先锁桶行：两人几乎同时对同一桶各登一笔时在此排队串行，
        // 锁内看到的余量是前一笔提交后的真实值，不依赖前端传值或内存计数。
        FuelBarrel barrel = lockBarrel(dto.getBarrelId());
        double levelBefore = normalize(barrel.getCurrentLevel());

        // 余量不足：不进账、不改余量，明确报出差多少升
        if (liters > levelBefore + EPS) {
            throw new IllegalArgumentException(String.format(
                    "桶[%s]当前余量 %.3f 升，不足本次领用 %.3f 升（缺 %.3f 升），本笔领用不能进账，余量保持 %.3f 升不变",
                    barrel.getBarrelNo(), levelBefore, liters, normalize(liters - levelBefore), levelBefore));
        }

        SurveyTeam team = surveyTeamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("领油小队不存在: " + dto.getTeamId()));
        TeamMember member = resolveMemberOfTeam(dto.getMemberId(), team);

        // 扣减落库：同一事务内改写余量 + 写台账，任一失败整体回滚
        double levelAfter = normalize(levelBefore - liters);
        if (levelAfter < 0 && levelAfter >= -EPS) {
            levelAfter = 0;
        }
        barrel.setCurrentLevel(levelAfter);
        barrelRepository.save(barrel);

        FuelLedger ledger = new FuelLedger();
        ledger.setBarrel(barrel);
        ledger.setTeam(team);
        ledger.setMember(member);
        ledger.setType(TYPE_ISSUE);
        ledger.setLiters(liters);
        ledger.setLedgerDate(date);
        ledger.setLevelAfter(levelAfter);
        ledger.setRemark(dto.getRemark());
        return ledgerRepository.save(ledger);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FuelLedger returnFuel(FuelReturnDTO dto) {
        double liters = requirePositive(dto.getLiters(), "回灌升数");
        LocalDate date = parseDate(dto.getReturnDate(), "回灌日期");

        FuelLedger origin = ledgerRepository.findById(dto.getIssueId())
                .orElseThrow(() -> new IllegalArgumentException("原领用记录不存在: " + dto.getIssueId()));
        if (!Objects.equals(origin.getType(), TYPE_ISSUE)) {
            throw new IllegalArgumentException("只能对领用记录进行回灌: 台账" + dto.getIssueId());
        }

        // 回灌必须倒回原领用的同一桶（桶、小队、队员均沿用原单，不接受改桶）
        FuelBarrel barrel = lockBarrel(origin.getBarrel().getId());
        double levelBefore = normalize(barrel.getCurrentLevel());
        double capacity = normalize(barrel.getRatedCapacity());

        // 回灌后会超过额定升数：整笔失败，余量停在领用扣完后的数，不能偷偷补一点
        if (levelBefore + liters > capacity + EPS) {
            throw new IllegalArgumentException(String.format(
                    "桶[%s]当前余量 %.3f 升，本次回灌 %.3f 升后将达到 %.3f 升，超过额定升数 %.3f 升（最多还能倒回 %.3f 升），本笔回灌失败，余量保持 %.3f 升不变",
                    barrel.getBarrelNo(), levelBefore, liters, normalize(levelBefore + liters),
                    capacity, normalize(capacity - levelBefore), levelBefore));
        }

        double levelAfter = normalize(levelBefore + liters);
        if (levelAfter > capacity && levelAfter <= capacity + EPS) {
            levelAfter = capacity;
        }
        barrel.setCurrentLevel(levelAfter);
        barrelRepository.save(barrel);

        FuelLedger ledger = new FuelLedger();
        ledger.setBarrel(barrel);
        ledger.setTeam(origin.getTeam());
        ledger.setMember(origin.getMember());
        ledger.setType(TYPE_RETURN);
        ledger.setLiters(liters);
        ledger.setLedgerDate(date);
        ledger.setRelatedIssueId(origin.getId());
        ledger.setLevelAfter(levelAfter);
        ledger.setRemark(dto.getRemark());
        return ledgerRepository.save(ledger);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuelLedger> findLedger(Long barrelId, Long teamId, Long memberId, Integer type) {
        boolean hasBarrel = barrelId != null;
        boolean hasTeam = teamId != null;
        boolean hasMember = memberId != null;
        boolean hasType = type != null;

        // 严格单条件走带明细查询；其余组合条件在带明细的全集上内存过滤
        if (!hasBarrel && !hasTeam && !hasMember && !hasType) {
            return ledgerRepository.findAllWithDetails();
        }
        if (hasBarrel && !hasTeam && !hasMember && !hasType) {
            return ledgerRepository.findWithDetailsByBarrelId(barrelId);
        }
        if (!hasBarrel && hasTeam && !hasMember && !hasType) {
            return ledgerRepository.findWithDetailsByTeamId(teamId);
        }
        if (!hasBarrel && !hasTeam && hasMember && !hasType) {
            return ledgerRepository.findWithDetailsByMemberId(memberId);
        }
        if (!hasBarrel && !hasTeam && !hasMember && hasType) {
            return ledgerRepository.findWithDetailsByType(type);
        }
        return ledgerRepository.findAllWithDetails().stream()
                .filter(l -> !hasBarrel || Objects.equals(l.getBarrel().getId(), barrelId))
                .filter(l -> !hasTeam || (l.getTeam() != null && Objects.equals(l.getTeam().getId(), teamId)))
                .filter(l -> !hasMember || (l.getMember() != null && Objects.equals(l.getMember().getId(), memberId)))
                .filter(l -> !hasType || Objects.equals(l.getType(), type))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FuelLedger findLedgerById(Long id) {
        return ledgerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("油料台账记录不存在: " + id));
    }
}
