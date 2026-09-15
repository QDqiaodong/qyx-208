package com.example.geological.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SurveyTeamDTO {

    private Long id;

    @NotBlank(message = "小队名称不能为空")
    private String teamName;

    @NotBlank(message = "小队编码不能为空")
    private String teamCode;

    private String leaderName;

    private String leaderPhone;

    private String description;

    @NotNull(message = "随队总承重上限不能为空")
    @DecimalMin(value = "0.0", message = "随队总承重上限不能为负数")
    private Double maxLoadCapacity;
}