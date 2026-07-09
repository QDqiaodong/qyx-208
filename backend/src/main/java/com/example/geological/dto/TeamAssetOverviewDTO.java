package com.example.geological.dto;

import lombok.Data;

import java.util.List;

@Data
public class TeamAssetOverviewDTO {

    private Long teamId;
    private String teamName;
    private String teamCode;
    private Integer workstationCount;
    private Double totalLoadCapacity;
    private List<WorkstationDTO> workstations;
}