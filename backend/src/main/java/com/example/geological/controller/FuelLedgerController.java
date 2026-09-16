package com.example.geological.controller;

import com.example.geological.dto.FuelIssueDTO;
import com.example.geological.dto.FuelLedgerDTO;
import com.example.geological.dto.FuelReturnDTO;
import com.example.geological.dto.ResponseDTO;
import com.example.geological.entity.FuelLedger;
import com.example.geological.service.FuelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 油料领用台账：领用出账、回灌入账、流水查询。
 * 余量在领用/回灌落账当时即在油桶上扣减/增加并持久化，
 * 不足领用、回灌超额都由后端在桶行悲观锁内原子判定失败。
 */
@RestController
@RequestMapping("/api/fuel-ledger")
@RequiredArgsConstructor
public class FuelLedgerController {

    private final FuelService fuelService;

    /** 台账流水，可按油桶、小队、队员、类型(1领用/2回灌)筛选 */
    @GetMapping
    public ResponseDTO<List<FuelLedgerDTO>> getAll(
            @RequestParam(required = false) Long barrelId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Integer type) {
        List<FuelLedger> ledger = fuelService.findLedger(barrelId, teamId, memberId, type);
        return ResponseDTO.success(ledger.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseDTO<FuelLedgerDTO> getById(@PathVariable Long id) {
        return ResponseDTO.success(toDTO(fuelService.findLedgerById(id)));
    }

    /** 出队领油登记：登记当时扣减桶余量 */
    @PostMapping("/issue")
    public ResponseDTO<FuelLedgerDTO> issue(@Valid @RequestBody FuelIssueDTO dto) {
        FuelLedger ledger = fuelService.issue(dto);
        return ResponseDTO.success(
                String.format("领用登记成功，桶[%s]余量 %.3f 升", ledger.getBarrel().getBarrelNo(), ledger.getLevelAfter()),
                toDTO(ledger));
    }

    /** 回灌：没用完的油倒回原领用同一桶 */
    @PostMapping("/return")
    public ResponseDTO<FuelLedgerDTO> returnFuel(@Valid @RequestBody FuelReturnDTO dto) {
        FuelLedger ledger = fuelService.returnFuel(dto);
        return ResponseDTO.success(
                String.format("回灌成功，桶[%s]余量 %.3f 升", ledger.getBarrel().getBarrelNo(), ledger.getLevelAfter()),
                toDTO(ledger));
    }

    private FuelLedgerDTO toDTO(FuelLedger l) {
        FuelLedgerDTO dto = new FuelLedgerDTO();
        dto.setId(l.getId());
        dto.setType(l.getType());
        dto.setLiters(l.getLiters());
        dto.setLedgerDate(l.getLedgerDate() != null ? l.getLedgerDate().toString() : null);
        dto.setRelatedIssueId(l.getRelatedIssueId());
        dto.setLevelAfter(l.getLevelAfter());
        dto.setRemark(l.getRemark());
        if (l.getBarrel() != null) {
            dto.setBarrelId(l.getBarrel().getId());
            dto.setBarrelNo(l.getBarrel().getBarrelNo());
        }
        if (l.getTeam() != null) {
            dto.setTeamId(l.getTeam().getId());
            dto.setTeamName(l.getTeam().getTeamName());
        }
        if (l.getMember() != null) {
            dto.setMemberId(l.getMember().getId());
            dto.setMemberNo(l.getMember().getMemberNo());
            dto.setMemberName(l.getMember().getMemberName());
        }
        return dto;
    }
}
