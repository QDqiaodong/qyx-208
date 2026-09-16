package com.example.geological.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 出队领油登记入参：哪一支小队、哪一名队员、从哪一桶舀走多少升、哪一天。
 */
@Data
public class FuelIssueDTO {

    @NotNull(message = "油桶不能为空")
    private Long barrelId;

    @NotNull(message = "领油小队不能为空")
    private Long teamId;

    @NotNull(message = "领油队员不能为空")
    private Long memberId;

    @NotNull(message = "领用升数不能为空")
    @Positive(message = "领用升数必须大于0")
    private Double liters;

    /** 领用日期 yyyy-MM-dd；不传默认当天 */
    private String issueDate;

    private String remark;
}
