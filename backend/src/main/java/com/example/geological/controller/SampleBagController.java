package com.example.geological.controller;

import com.example.geological.dto.ResponseDTO;
import com.example.geological.dto.SampleBagDTO;
import com.example.geological.entity.SampleBag;
import com.example.geological.service.SampleBagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sample-bags")
@RequiredArgsConstructor
public class SampleBagController {

    private final SampleBagService sampleBagService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 台账列表，可按小队、队员、状态筛选。
     */
    @GetMapping
    public ResponseDTO<List<SampleBagDTO>> getAll(
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Integer status) {
        List<SampleBag> bags = sampleBagService.findAll(teamId, memberId, status);
        return ResponseDTO.success(bags.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseDTO<SampleBagDTO> getById(@PathVariable Long id) {
        return ResponseDTO.success(convertToDTO(sampleBagService.findById(id)));
    }

    /** 送检登记（落账） */
    @PostMapping
    public ResponseDTO<SampleBagDTO> register(@Valid @RequestBody SampleBagDTO dto) {
        SampleBag bag = sampleBagService.register(dto);
        return ResponseDTO.success("送检登记成功，已在途", convertToDTO(bag));
    }

    /** 修改登记；办结袋直接修改会失败，须先退回在途 */
    @PutMapping("/{id}")
    public ResponseDTO<SampleBagDTO> update(@PathVariable Long id, @Valid @RequestBody SampleBagDTO dto) {
        SampleBag bag = sampleBagService.update(id, dto);
        return ResponseDTO.success("更新成功", convertToDTO(bag));
    }

    /** 出站办结：在途 -> 办结 */
    @PutMapping("/{id}/complete")
    public ResponseDTO<SampleBagDTO> complete(@PathVariable Long id) {
        return ResponseDTO.success("已出站办结", convertToDTO(sampleBagService.complete(id)));
    }

    /** 退回在途：办结 -> 在途，之后才能改小队/袋重 */
    @PutMapping("/{id}/return-transit")
    public ResponseDTO<SampleBagDTO> returnToTransit(@PathVariable Long id) {
        return ResponseDTO.success("已退回在途，可修改后重新办结", convertToDTO(sampleBagService.returnToTransit(id)));
    }

    private SampleBagDTO convertToDTO(SampleBag bag) {
        SampleBagDTO dto = new SampleBagDTO();
        dto.setId(bag.getId());
        dto.setBagNo(bag.getBagNo());
        dto.setBagWeight(bag.getBagWeight());
        dto.setSubmitDate(bag.getSubmitDate() != null ? bag.getSubmitDate().toString() : null);
        dto.setStatus(bag.getStatus());
        if (bag.getTeam() != null) {
            dto.setTeamId(bag.getTeam().getId());
            dto.setTeamName(bag.getTeam().getTeamName());
        }
        if (bag.getMember() != null) {
            dto.setMemberId(bag.getMember().getId());
            dto.setMemberNo(bag.getMember().getMemberNo());
            dto.setMemberName(bag.getMember().getMemberName());
        }
        LocalDateTime completeTime = bag.getCompleteTime();
        if (completeTime != null) {
            dto.setCompleteTime(completeTime.format(TIME_FMT));
        }
        return dto;
    }
}
