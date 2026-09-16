package com.example.geological.controller;

import com.example.geological.dto.RainDailyTotalDTO;
import com.example.geological.dto.ResponseDTO;
import com.example.geological.service.RainfallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 气象卡片：每个自然日一行的合计毫米数。
 * 合计随读数的记/改/作废在同一事务内重算落库，读到的永远是
 * 截至当前已提交的有效读数之和，不会是早上那一笔的旧数。
 */
@RestController
@RequestMapping("/api/rain-cards")
@RequiredArgsConstructor
public class RainCardController {

    private final RainfallService rainfallService;

    /** 卡片列表，可按日期区间(from/to, yyyy-MM-dd)筛选；不传返回全部 */
    @GetMapping
    public ResponseDTO<List<RainDailyTotalDTO>> getAll(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseDTO.success(rainfallService.findCards(from, to));
    }
}
