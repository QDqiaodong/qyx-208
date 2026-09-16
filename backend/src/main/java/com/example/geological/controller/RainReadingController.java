package com.example.geological.controller;

import com.example.geological.dto.RainReadingDTO;
import com.example.geological.dto.RainReadingRecordDTO;
import com.example.geological.dto.RainReadingUpdateDTO;
import com.example.geological.dto.RainReadingVoidDTO;
import com.example.geological.dto.ResponseDTO;
import com.example.geological.entity.RainReading;
import com.example.geological.service.RainfallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 雨量读数（rec 本）：记一笔、改毫米数、作废、流水查询。
 * 记/改/作废都与气象卡片当日合计的重算在同一事务内落库；
 * 改数、作废必须带版本号，两人抢着动同一桶同一日只成一次。
 */
@RestController
@RequestMapping("/api/rain-readings")
@RequiredArgsConstructor
public class RainReadingController {

    private final RainfallService rainfallService;

    /** rec 本流水，可按桶、自然日(yyyy-MM-dd)、状态(1有效/2作废)筛选 */
    @GetMapping
    public ResponseDTO<List<RainReadingDTO>> getAll(
            @RequestParam(required = false) Long bucketId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer status) {
        List<RainReading> readings = rainfallService.findReadings(bucketId, date, status);
        return ResponseDTO.success(readings.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /** 记一笔：桶号、自然日、本次毫米数；同桶同日已有有效读数则整笔失败 */
    @PostMapping
    public ResponseDTO<RainReadingDTO> record(@Valid @RequestBody RainReadingRecordDTO dto) {
        RainReading reading = rainfallService.record(dto);
        return ResponseDTO.success(
                String.format("登记成功，桶[%s] %s 记 %.1f 毫米，气象卡片当日合计已重算",
                        reading.getBucket().getBucketNo(), reading.getReadDate(), reading.getMillimeters()),
                toDTO(reading));
    }

    /** 改毫米数：必须带版本号；改数成功的同时重算当日卡片合计 */
    @PutMapping("/{id}")
    public ResponseDTO<RainReadingDTO> updateMillimeters(@PathVariable Long id,
                                                         @Valid @RequestBody RainReadingUpdateDTO dto) {
        RainReading reading = rainfallService.updateMillimeters(id, dto);
        return ResponseDTO.success(
                String.format("改数成功，桶[%s] %s 改为 %.1f 毫米，气象卡片当日合计已重算",
                        reading.getBucket().getBucketNo(), reading.getReadDate(), reading.getMillimeters()),
                toDTO(reading));
    }

    /** 作废：必须留作废原因；作废后当日卡片合计立即不再计入这一笔 */
    @PostMapping("/{id}/void")
    public ResponseDTO<RainReadingDTO> voidReading(@PathVariable Long id,
                                                   @Valid @RequestBody RainReadingVoidDTO dto) {
        RainReading reading = rainfallService.voidReading(id, dto);
        return ResponseDTO.success(
                String.format("已作废，桶[%s] %s 的 %.1f 毫米不再计入当日合计",
                        reading.getBucket().getBucketNo(), reading.getReadDate(), reading.getMillimeters()),
                toDTO(reading));
    }

    private RainReadingDTO toDTO(RainReading r) {
        RainReadingDTO dto = new RainReadingDTO();
        dto.setId(r.getId());
        dto.setReadDate(r.getReadDate() != null ? r.getReadDate().toString() : null);
        dto.setMillimeters(r.getMillimeters());
        dto.setStatus(r.getStatus());
        dto.setVoidReason(r.getVoidReason());
        dto.setVoidTime(r.getVoidTime() != null ? r.getVoidTime().toString() : null);
        dto.setVersion(r.getVersion());
        dto.setRemark(r.getRemark());
        if (r.getBucket() != null) {
            dto.setBucketId(r.getBucket().getId());
            dto.setBucketNo(r.getBucket().getBucketNo());
        }
        return dto;
    }
}
