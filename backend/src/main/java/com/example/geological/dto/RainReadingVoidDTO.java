package com.example.geological.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作废读数入参。作废必须留下原因（永久留痕），并带上看到的版本号：
 * 与改数一样，两名值班抢着动同一桶同一日时只成一次。
 */
@Data
public class RainReadingVoidDTO {

    /** 提交人手里这条读数的版本号（列表里查到的 version） */
    @NotNull(message = "缺少读数版本号，请刷新列表后再作废")
    private Integer expectedVersion;

    /** 作废原因（必填，留痕） */
    @NotBlank(message = "作废原因不能为空")
    private String voidReason;
}
