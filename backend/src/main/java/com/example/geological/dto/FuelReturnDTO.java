package com.example.geological.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 回灌入参：把没用完的油倒回同一桶。
 *
 * issueId 必填：回灌挂到原领用台账上，桶号/小队/队员均沿用原领用单，
 * 从根本上保证「倒回同一桶」。回灌升数加到余量上，但不得超过额定升数；
 * 校验失败时整笔回滚，余量停在领用扣完后的数。
 */
@Data
public class FuelReturnDTO {

    @NotNull(message = "原领用记录不能为空")
    private Long issueId;

    @NotNull(message = "回灌升数不能为空")
    @Positive(message = "回灌升数必须大于0")
    private Double liters;

    /** 回灌日期 yyyy-MM-dd；不传默认当天 */
    private String returnDate;

    private String remark;
}
