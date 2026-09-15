package com.example.geological.service.impl;

import com.example.geological.dto.SampleBagDTO;
import com.example.geological.entity.SampleBag;
import com.example.geological.entity.SurveyTeam;
import com.example.geological.entity.TeamMember;
import com.example.geological.repository.SampleBagRepository;
import com.example.geological.repository.SurveyTeamRepository;
import com.example.geological.repository.TeamMemberRepository;
import com.example.geological.service.SampleBagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SampleBagServiceImpl implements SampleBagService {

    private final SampleBagRepository sampleBagRepository;
    private final SurveyTeamRepository surveyTeamRepository;
    private final TeamMemberRepository memberRepository;

    /** 在途 */
    private static final int STATUS_IN_TRANSIT = 1;
    /** 办结（出站） */
    private static final int STATUS_COMPLETED = 2;

    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("送检日不能为空");
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("送检日格式不正确，应为 yyyy-MM-dd: " + date);
        }
    }

    private Double validateWeight(Double weight) {
        if (weight == null || weight <= 0) {
            throw new IllegalArgumentException("袋重必须大于0");
        }
        return weight;
    }

    /**
     * 悲观写锁锁定小队行。所有会改变某小队送检台账（在途袋）的写路径都先拿这把锁，
     * 再做同小队/同日/同袋号的查重，保证两人几乎同时交同一袋时只有先到的一条进账。
     */
    private SurveyTeam lockTeam(Long teamId) {
        return surveyTeamRepository.findByIdForUpdate(teamId)
                .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + teamId));
    }

    /**
     * 校验送检队员存在、在队、且属于所选小队。
     */
    private TeamMember resolveMemberOfTeam(Long memberId, SurveyTeam team) {
        TeamMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("送检队员不存在: " + memberId));
        if (!Objects.equals(member.getStatus(), 1)) {
            throw new IllegalArgumentException("送检队员已停用，不能登记送检: " + member.getMemberNo());
        }
        if (member.getTeam() == null || !Objects.equals(member.getTeam().getId(), team.getId())) {
            throw new IllegalArgumentException(String.format(
                    "送检队员[%s]不属于小队[%s]，不能由该小队送检",
                    member.getMemberName(), team.getTeamName()));
        }
        return member;
    }

    /**
     * 同小队、同送检日、同袋号查重。excludeId 用于修改时排除自身。
     * 命中即在途或办结任一条都拒绝，绝不自动作废旧袋，也绝不允许改名后顶替。
     */
    private void assertNoDuplicate(Long teamId, LocalDate date, String bagNo, Long excludeId) {
        Optional<SampleBag> existing =
                sampleBagRepository.findByTeamIdAndSubmitDateAndBagNo(teamId, date, bagNo);
        if (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            SampleBag bag = existing.get();
            String stateDesc = Objects.equals(bag.getStatus(), STATUS_COMPLETED) ? "已办结" : "在途";
            throw new IllegalArgumentException(String.format(
                    "袋号[%s]在小队[%s] %s 的送检登记已存在（%s，送检队员%s），该袋已在途/办结，本次登记失败，不允许重复落账",
                    bagNo, bag.getTeam().getTeamName(), date, stateDesc,
                    bag.getMember().getMemberName()));
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SampleBag register(SampleBagDTO dto) {
        LocalDate date = parseDate(dto.getSubmitDate());
        Double weight = validateWeight(dto.getBagWeight());

        // 先锁小队行，把同小队的并发送检串行化，确保「查重 + 落账」原子
        SurveyTeam team = lockTeam(dto.getTeamId());
        TeamMember member = resolveMemberOfTeam(dto.getMemberId(), team);
        assertNoDuplicate(team.getId(), date, dto.getBagNo().trim(), null);

        SampleBag bag = new SampleBag();
        bag.setBagNo(dto.getBagNo().trim());
        bag.setTeam(team);
        bag.setMember(member);
        bag.setBagWeight(weight);
        bag.setSubmitDate(date);
        bag.setStatus(STATUS_IN_TRANSIT);

        return saveWithDuplicateGuard(bag, team, date, bag.getBagNo());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SampleBag update(Long id, SampleBagDTO dto) {
        SampleBag bag = sampleBagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("样品袋登记不存在: " + id));

        // 已出站办结的袋子整体冻结：不能改小队、不能改袋重（也不能改袋号/送检日），
        // 必须先退回在途再改。直接改小队本身就要失败，不能留下办结后被挪队的记录。
        if (Objects.equals(bag.getStatus(), STATUS_COMPLETED)) {
            throw new IllegalArgumentException(String.format(
                    "袋号[%s]已出站办结，不能直接修改小队或袋重，请先退回在途后再修改",
                    bag.getBagNo()));
        }

        LocalDate date = parseDate(dto.getSubmitDate());
        Double weight = validateWeight(dto.getBagWeight());

        SurveyTeam team = lockTeam(dto.getTeamId());
        TeamMember member = resolveMemberOfTeam(dto.getMemberId(), team);
        // 不允许悄悄改袋号/日期让本次提交蒙混过关：改后若与同队同日另一袋撞号，同样失败
        assertNoDuplicate(team.getId(), date, dto.getBagNo().trim(), id);

        bag.setBagNo(dto.getBagNo().trim());
        bag.setTeam(team);
        bag.setMember(member);
        bag.setBagWeight(weight);
        bag.setSubmitDate(date);

        return saveWithDuplicateGuard(bag, team, date, bag.getBagNo());
    }

    /**
     * 唯一索引兜底：理论上同小队的并发登记已被小队行锁串行化，
     * 这里再捕获数据库唯一约束冲突，给后到者返回「哪只袋已在途」的明确信息，
     * 而不是抛出笼统的数据库错误。绝不作废旧袋、绝不改名顶替。
     */
    private SampleBag saveWithDuplicateGuard(SampleBag bag, SurveyTeam team, LocalDate date, String bagNo) {
        try {
            return sampleBagRepository.save(bag);
        } catch (DataIntegrityViolationException e) {
            log.warn("样品袋唯一约束冲突 teamId={} date={} bagNo={}", team.getId(), date, bagNo, e);
            throw new IllegalArgumentException(String.format(
                    "袋号[%s]在小队[%s] %s 的送检登记已存在，该袋已在途，先到的登记已进账，本次提交失败",
                    bagNo, team.getTeamName(), date));
        }
    }

    @Override
    @Transactional
    public SampleBag complete(Long id) {
        SampleBag bag = sampleBagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("样品袋登记不存在: " + id));
        if (Objects.equals(bag.getStatus(), STATUS_COMPLETED)) {
            throw new IllegalArgumentException("袋号[" + bag.getBagNo() + "]已办结，无需重复办结");
        }
        bag.setStatus(STATUS_COMPLETED);
        bag.setCompleteTime(java.time.LocalDateTime.now());
        return sampleBagRepository.save(bag);
    }

    @Override
    @Transactional
    public SampleBag returnToTransit(Long id) {
        SampleBag bag = sampleBagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("样品袋登记不存在: " + id));
        if (Objects.equals(bag.getStatus(), STATUS_IN_TRANSIT)) {
            throw new IllegalArgumentException("袋号[" + bag.getBagNo() + "]当前在途，无需退回");
        }
        // 退回在途后方可改小队/袋重；办结信息清空
        bag.setStatus(STATUS_IN_TRANSIT);
        bag.setCompleteTime(null);
        return sampleBagRepository.save(bag);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleBag findById(Long id) {
        return sampleBagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("样品袋登记不存在: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleBag> findAll(Long teamId, Long memberId, Integer status) {
        boolean hasTeam = teamId != null;
        boolean hasMember = memberId != null;
        boolean hasStatus = status != null;

        if (!hasTeam && !hasMember && !hasStatus) {
            return sampleBagRepository.findAllWithDetails();
        }
        if (hasTeam && !hasMember && !hasStatus) {
            return sampleBagRepository.findWithDetailsByTeamId(teamId);
        }
        if (!hasTeam && hasMember && !hasStatus) {
            return sampleBagRepository.findWithDetailsByMemberId(memberId);
        }
        if (!hasTeam && !hasMember) {
            return sampleBagRepository.findWithDetailsByStatus(status);
        }
        // 组合条件在内存过滤（带明细查询保证关联可用）
        return sampleBagRepository.findAllWithDetails().stream()
                .filter(b -> !hasTeam || Objects.equals(b.getTeam().getId(), teamId))
                .filter(b -> !hasMember || Objects.equals(b.getMember().getId(), memberId))
                .filter(b -> !hasStatus || Objects.equals(b.getStatus(), status))
                .toList();
    }

    @Override
    public long countInTransitByTeamId(Long teamId) {
        return sampleBagRepository.countInTransitByTeamId(teamId);
    }

    @Override
    public double sumInTransitWeightByTeamId(Long teamId) {
        return sampleBagRepository.sumInTransitWeightByTeamId(teamId);
    }
}
