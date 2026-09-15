package com.example.geological.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 样品袋送检登记入参。
 * 送检日格式：yyyy-MM-dd（前端 el-date-picker 默认按 ISO 日期提交）。
 */
@Data
public class SampleBagDTO {

    private Long id;

    @NotBlank(message = "袋号不能为空")
    private String bagNo;

    @NotNull(message = "所属小队不能为空")
    private Long teamId;

    @NotNull(message = "送检队员不能为空")
    private Long memberId;

    @NotNull(message = "袋重不能为空")
    @Positive(message = "袋重必须大于0")
    private Double bagWeight;

    @NotNull(message = "送检日不能为空")
    private String submitDate;

    private String teamName;

    private String memberNo;

    private String memberName;

    /** 1=在途，2=办结；登记时可不传，默认在途 */
    private Integer status;

    private String completeTime;
}
