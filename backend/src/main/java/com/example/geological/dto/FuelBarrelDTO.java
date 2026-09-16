package com.example.geological.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 油桶建档入参：桶号、额定升数。
 * 当前余量建档时默认等于额定升数（新桶满桶入库）。
 */
@Data
public class FuelBarrelDTO {

    private Long id;

    @NotBlank(message = "桶号不能为空")
    private String barrelNo;

    @NotNull(message = "额定升数不能为空")
    @Positive(message = "额定升数必须大于0")
    private Double ratedCapacity;

    /** 当前余量；建档时可不传，默认满桶（=额定升数） */
    private Double currentLevel;

    private String remark;
}
