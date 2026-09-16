package com.example.geological.dto;

import lombok.Data;

/**
 * 气象卡片出参：一个自然日的合计毫米数。
 * 合计 = 该日各桶有效读数之和，随读数改动在同一事务内重算落库。
 */
@Data
public class RainDailyTotalDTO {

    /** 自然日 yyyy-MM-dd */
    private String readDate;

    /** 当日合计毫米数（只加有效读数，作废不计） */
    private Double totalMm;

    /** 参与合计的有效读数条数 */
    private Integer validCount;

    /** 卡片最近一次重算时间 */
    private String updateTime;
}
