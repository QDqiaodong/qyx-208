package com.example.geological.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * 记一笔雨量读数的入参：哪个桶、哪个自然日、本次多少毫米。
 */
@Data
public class RainReadingRecordDTO {

    @NotNull(message = "收集桶不能为空")
    private Long bucketId;

    /** 自然日 yyyy-MM-dd；不传默认当天 */
    private String readDate;

    @NotNull(message = "毫米数不能为空")
    @PositiveOrZero(message = "毫米数不能为负数")
    private Double millimeters;

    private String remark;
}
