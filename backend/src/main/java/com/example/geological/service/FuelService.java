package com.example.geological.service;

import com.example.geological.dto.FuelBarrelDTO;
import com.example.geological.dto.FuelIssueDTO;
import com.example.geological.dto.FuelReturnDTO;
import com.example.geological.entity.FuelBarrel;
import com.example.geological.entity.FuelLedger;

import java.util.List;

/**
 * 营地柴油领用台账：油桶档案 + 领用/回灌流水。
 * 与操作台挂靠、转队通知、队员停用三条链完全独立。
 */
public interface FuelService {

    /** 油桶建档：桶号、额定升数；余量默认满桶 */
    FuelBarrel createBarrel(FuelBarrelDTO dto);

    List<FuelBarrel> findAllBarrels();

    FuelBarrel findBarrelById(Long id);

    /**
     * 出队领油：登记小队、队员、桶、升数、日期，登记当时即扣减该桶余量。
     * 桶余量不足时整笔失败（台账不落、余量不变）。
     */
    FuelLedger issue(FuelIssueDTO dto);

    /**
     * 回灌：把没用完的油倒回原领用同一桶，余量增加但不得超过额定升数。
     * 回灌不成立时整笔失败，余量停在领用扣完后的数。
     */
    FuelLedger returnFuel(FuelReturnDTO dto);

    /** 台账流水，可按桶、小队、队员、类型筛选 */
    List<FuelLedger> findLedger(Long barrelId, Long teamId, Long memberId, Integer type);

    FuelLedger findLedgerById(Long id);
}
