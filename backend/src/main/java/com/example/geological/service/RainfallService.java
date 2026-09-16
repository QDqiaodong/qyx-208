package com.example.geological.service;

import com.example.geological.dto.RainBucketDTO;
import com.example.geological.dto.RainDailyTotalDTO;
import com.example.geological.dto.RainReadingRecordDTO;
import com.example.geological.dto.RainReadingUpdateDTO;
import com.example.geological.dto.RainReadingVoidDTO;
import com.example.geological.entity.RainBucket;
import com.example.geological.entity.RainReading;

import java.util.List;

/**
 * 营地雨量记录：收集桶档案 + 雨量读数（rec 本）+ 气象卡片当日合计。
 * 与操作台挂靠、转队通知、队员停用、油料台账各条链完全独立。
 *
 * 三条硬规矩：
 * 1) 同一桶同一自然日只留一条有效读数；
 * 2) 记一笔/改数/作废与气象卡片当日合计的重算在同一事务内落库，
 *    做到一半中断则整体回滚，卡片合计仍是动手前的数；
 * 3) 改数/作废必须带上看到的版本号，两人抢着动同一桶同一日只成一次。
 */
public interface RainfallService {

    /** 收集桶建档：桶号唯一 */
    RainBucket createBucket(RainBucketDTO dto);

    List<RainBucket> findAllBuckets();

    /**
     * 记一笔：桶号、自然日、本次毫米数。
     * 同桶同日已有有效读数时整笔失败（可对其改数或作废）；落账同时重算当日卡片合计。
     */
    RainReading record(RainReadingRecordDTO dto);

    /**
     * 改毫米数：只许改有效读数；版本号对不上（他人抢先改过/作废）即 409 失败。
     * 改数成功的同时按各桶有效读数重算当日卡片合计。
     */
    RainReading updateMillimeters(Long id, RainReadingUpdateDTO dto);

    /**
     * 作废：必须留作废原因；作废后当日卡片合计立即不再计入这一笔。
     * 作废行永久留痕，不能改数、不能重复作废；该桶该日可重新记一笔。
     */
    RainReading voidReading(Long id, RainReadingVoidDTO dto);

    /** rec 本流水，可按桶、自然日、状态筛选 */
    List<RainReading> findReadings(Long bucketId, String readDate, Integer status);

    /** 气象卡片：各自然日合计（可按日期区间筛选） */
    List<RainDailyTotalDTO> findCards(String from, String to);
}
