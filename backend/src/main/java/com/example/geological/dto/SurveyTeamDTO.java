package com.example.geological.dto;

import jakarta.validation.constraints.NotBlank;
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
}