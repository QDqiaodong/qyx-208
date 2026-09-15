package com.example.geological.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkstationDTO {

    private Long id;

    @NotBlank(message = "操作台编号不能为空")
    private String workstationNo;

    @NotBlank(message = "承重不能为空")
    private String loadCapacity;

    private String workstationType;

    private String adaptStation;

    private Long currentTeamId;

    private String currentTeamName;

    private Integer status;
}