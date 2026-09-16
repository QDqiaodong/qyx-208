package com.example.geological.dto;

import lombok.Data;

/**
 * 雨水收集桶档案：桶号 + 备注。建档入参与出参共用。
 */
@Data
public class RainBucketDTO {

    private Long id;

    /** 桶号（桶上贴的编号，全营唯一） */
    private String bucketNo;

    private String remark;
}
