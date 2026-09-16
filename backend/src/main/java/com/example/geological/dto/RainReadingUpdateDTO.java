package com.example.geological.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * 改毫米数入参。必须带上值班在页面上看到的版本号：
 * 两名值班抢着改同一桶同一日时，先提交的生效，后提交的版本对不上即失败（409），只成一次。
 */
@Data
public class RainReadingUpdateDTO {

    @NotNull(message = "毫米数不能为空")
    @PositiveOrZero(message = "毫米数不能为负数")
    private Double millimeters;

    /** 提交人手里这条读数的版本号（列表里查到的 version） */
    @NotNull(message = "缺少读数版本号，请刷新列表后再改")
    private Integer expectedVersion;
}
