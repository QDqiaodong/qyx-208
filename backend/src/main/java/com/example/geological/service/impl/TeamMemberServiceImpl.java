package com.example.geological.service.impl;

import com.example.geological.dto.TeamMemberDTO;
import com.example.geological.entity.SurveyTeam;
import com.example.geological.entity.TeamMember;
import com.example.geological.exception.MemberDepartedException;
import com.example.geological.repository.SurveyTeamRepository;
import com.example.geological.repository.TeamMemberRepository;
import com.example.geological.service.TeamMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamMemberServiceImpl implements TeamMemberService {

    /** 在职 */
    private static final int STATUS_ACTIVE = 1;
    /** 停用（离队） */
    private static final int STATUS_DEPARTED = 0;

    private final TeamMemberRepository memberRepository;
    private final SurveyTeamRepository surveyTeamRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TeamMember create(TeamMemberDTO dto) {
        // 编号全局唯一：即使原持号人已停用离队，也不允许用同号再建一个顶替
        memberRepository.findByMemberNo(dto.getMemberNo())
                .ifPresent(m -> {
                    throw new IllegalArgumentException("队员编号已存在: " + dto.getMemberNo());
                });

        SurveyTeam team = surveyTeamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + dto.getTeamId()));

        TeamMember member = new TeamMember();
        member.setMemberNo(dto.getMemberNo());
        member.setMemberName(dto.getMemberName());
        member.setPhone(dto.getPhone());
        member.setPosition(dto.getPosition());
        member.setTeam(team);

        return memberRepository.save(member);
    }

    /**
     * 停用队员（已离队）整条档案冻结：不能改挂到任何小队，也不能改姓名/电话/职位/编号。
     * 想重新挂队必须走重新入职建档，绝不通过编辑自动恢复在职，
     * 也绝不靠悄悄改队员编号让本次改挂蒙混过关。
     *
     * 先对队员行加悲观写锁，再在锁内复查 status：两人几乎同时把同一名停用队员
     * 改挂到两支不同小队时被串行化，两边复查都看到 status=0，两边都失败，
     * 不会留下停用后还被两队同时认领的记录。
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TeamMember update(Long id, TeamMemberDTO dto) {
        TeamMember member = memberRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("队员不存在: " + id));

        if (Objects.equals(member.getStatus(), STATUS_DEPARTED)) {
            String teamName = member.getTeam() != null ? member.getTeam().getTeamName() : "无";
            throw new MemberDepartedException(String.format(
                    "队员[%s（%s）]已停用离队（原小队：%s），不能再改挂到任何小队，也不能编辑其档案；"
                            + "如需重新派驻，请按重新入职建档处理，本次改挂未落账",
                    member.getMemberName(), member.getMemberNo(), teamName));
        }

        // 在职队员改编号：不允许改成他人（含已停用离队人员）占用的编号
        if (!member.getMemberNo().equals(dto.getMemberNo())) {
            memberRepository.findByMemberNo(dto.getMemberNo())
                    .ifPresent(m -> {
                        throw new IllegalArgumentException("队员编号已存在: " + dto.getMemberNo());
                    });
            member.setMemberNo(dto.getMemberNo());
        }

        member.setMemberName(dto.getMemberName());
        member.setPhone(dto.getPhone());
        member.setPosition(dto.getPosition());

        if (dto.getTeamId() != null && (member.getTeam() == null
                || !dto.getTeamId().equals(member.getTeam().getId()))) {
            SurveyTeam team = surveyTeamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("小队不存在: " + dto.getTeamId()));
            // 锁内复查 status：若在拿到队员行锁前的间隙被另一事务停用，则拒绝，绝不落账改挂。
            // 队员行悲观写锁保证两人几乎同时把同一停用队员改挂到两队时在此串行，两边都失败。
            if (Objects.equals(member.getStatus(), STATUS_DEPARTED)) {
                throw new MemberDepartedException(String.format(
                        "队员[%s（%s）]已停用离队，本次改挂未落账",
                        member.getMemberName(), member.getMemberNo()));
            }
            member.setTeam(team);
        }

        return memberRepository.save(member);
    }

    /**
     * 停用即离队：只置 status=0，不断开原小队关联（保留历史归属），
     * 但此后一切在职口径（小队花名册、在职人数、按队员查资产、改挂）都不再认这个人。
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long id) {
        TeamMember member = memberRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("队员不存在: " + id));
        if (Objects.equals(member.getStatus(), STATUS_DEPARTED)) {
            throw new MemberDepartedException(String.format(
                    "队员[%s（%s）]已停用离队，无需重复停用",
                    member.getMemberName(), member.getMemberNo()));
        }
        member.setStatus(STATUS_DEPARTED);
        memberRepository.save(member);
    }

    @Override
    public TeamMember findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("队员不存在: " + id));
    }

    /**
     * 按队员编号取「在职」队员（带所属小队）。
     * 编号不存在 -> 队员不存在；编号存在但已停用 -> 明确报已离队，
     * 绝不返回其原小队操作台/承重等任何资产数据。
     */
    @Override
    public TeamMember findByMemberNo(String memberNo) {
        Optional<TeamMember> existing = memberRepository.findByMemberNo(memberNo);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("队员不存在: " + memberNo);
        }
        TeamMember member = existing.get();
        if (!Objects.equals(member.getStatus(), STATUS_ACTIVE)) {
            throw new MemberDepartedException(String.format(
                    "队员[%s（%s）]已停用离队，不再属于任何在职小队，"
                            + "不能查询其原小队操作台、承重等资产清单",
                    member.getMemberName(), memberNo));
        }
        return memberRepository.findActiveWithTeamByMemberNo(memberNo)
                .orElseThrow(() -> new MemberDepartedException(String.format(
                        "队员[%s]已停用离队，不能查询其原小队资产清单", memberNo)));
    }

    @Override
    public List<TeamMember> findByTeamId(Long teamId) {
        // 小队花名册只认在职队员；停用离队者不得再作为在职队员出现
        return memberRepository.findByTeamIdAndStatus(teamId, STATUS_ACTIVE);
    }

    @Override
    public List<TeamMember> findAll() {
        return memberRepository.findAll().stream()
                .filter(m -> Objects.equals(m.getStatus(), STATUS_ACTIVE))
                .toList();
    }

    @Override
    public List<TeamMember> findActiveByTeamId(Long teamId) {
        return memberRepository.findByTeamIdAndStatus(teamId, STATUS_ACTIVE);
    }
}
