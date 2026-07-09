package com.example.geological.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferDTO {

    @NotNull(message = "操作台ID不能为空")
    private Long workstationId;

    @NotNull(message = "目标小队ID不能为空")
    private Long toTeamId;

    private String transferReason;

    private String operator;
}