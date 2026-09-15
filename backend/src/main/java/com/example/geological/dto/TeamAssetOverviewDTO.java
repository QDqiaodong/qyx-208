package com.example.geological.dto;

import lombok.Data;

import java.util.List;

@Data
public class TeamAssetOverviewDTO {

    private Long teamId;
    private String teamName;
    private String teamCode;
    private Integer workstationCount;
    /** 随队总承重上限(kg) */
    private Double maxLoadCapacity;
    /** 已用承重(kg)，仅统计在用操作台 */
    private Double totalLoadCapacity;
    /** 剩余承重(kg) = 上限 - 已用 */
    private Double remainingLoadCapacity;
    /** 在途样品袋数（已登记送检、尚未出站办结） */
    private Long inTransitBagCount;
    /** 在途样品袋袋重合计(kg) */
    private Double inTransitBagWeight;
    private List<WorkstationDTO> workstations;
}