package com.example.geological.controller;

import com.example.geological.dto.RainBucketDTO;
import com.example.geological.dto.ResponseDTO;
import com.example.geological.entity.RainBucket;
import com.example.geological.service.RainfallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 雨水收集桶档案：桶号全营唯一。
 */
@RestController
@RequestMapping("/api/rain-buckets")
@RequiredArgsConstructor
public class RainBucketController {

    private final RainfallService rainfallService;

    @GetMapping
    public ResponseDTO<List<RainBucketDTO>> getAll() {
        List<RainBucket> buckets = rainfallService.findAllBuckets();
        return ResponseDTO.success(buckets.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseDTO<RainBucketDTO> create(@RequestBody RainBucketDTO dto) {
        RainBucket bucket = rainfallService.createBucket(dto);
        return ResponseDTO.success("收集桶建档成功", toDTO(bucket));
    }

    private RainBucketDTO toDTO(RainBucket bucket) {
        RainBucketDTO dto = new RainBucketDTO();
        dto.setId(bucket.getId());
        dto.setBucketNo(bucket.getBucketNo());
        dto.setRemark(bucket.getRemark());
        return dto;
    }
}
