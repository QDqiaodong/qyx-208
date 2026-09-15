package com.example.geological.service;

import com.example.geological.dto.SampleBagDTO;
import com.example.geological.entity.SampleBag;

import java.util.List;

public interface SampleBagService {

    /** 送检登记（落账） */
    SampleBag register(SampleBagDTO dto);

    /** 修改登记；办结袋不允许直接改，必须先退回在途 */
    SampleBag update(Long id, SampleBagDTO dto);

    /** 出站办结：在途 -> 办结 */
    SampleBag complete(Long id);

    /** 退回在途：办结 -> 在途，之后才能改小队/袋重 */
    SampleBag returnToTransit(Long id);

    SampleBag findById(Long id);

    List<SampleBag> findAll(Long teamId, Long memberId, Integer status);

    long countInTransitByTeamId(Long teamId);

    double sumInTransitWeightByTeamId(Long teamId);
}
