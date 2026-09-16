package com.example.geological.dto;

import lombok.Data;

/**
 * 油料台账流水出参。领用、回灌共用：type=1 领用，type=2 回灌。
 */
@Data
public class FuelLedgerDTO {

    private Long id;

    /** 1=领用，2=回灌 */
    private Integer type;

    private Long barrelId;

    private String barrelNo;

    private Long teamId;

    private String teamName;

    private Long memberId;

    private String memberNo;

    private String memberName;

    /** 本次升数（领用为舀走量，回灌为倒回量） */
    private Double liters;

    /** 登记日期 yyyy-MM-dd */
    private String ledgerDate;

    /** 回灌时关联的原领用台账 id */
    private Long relatedIssueId;

    /** 操作后该桶余量快照 */
    private Double levelAfter;

    private String remark;
}
