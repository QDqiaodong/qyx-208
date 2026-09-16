package com.example.geological.dto;

import lombok.Data;

/**
 * 雨量读数出参（rec 本一行，含作废留痕和当前版本号）。
 */
@Data
public class RainReadingDTO {

    private Long id;

    private Long bucketId;

    /** 桶号 */
    private String bucketNo;

    /** 自然日 yyyy-MM-dd */
    private String readDate;

    /** 本次毫米数 */
    private Double millimeters;

    /** 状态：1=有效，2=作废 */
    private Integer status;

    /** 作废原因（作废行留痕） */
    private String voidReason;

    /** 作废时间 */
    private String voidTime;

    /** 当前版本号：改数/作废时必须原样带回 */
    private Integer version;

    private String remark;
}
