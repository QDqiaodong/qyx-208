package com.example.geological.service;

import com.example.geological.dto.WorkstationDTO;
import com.example.geological.dto.TransferDTO;
import com.example.geological.entity.Workstation;
import com.example.geological.entity.WorkstationTransfer;

import java.util.List;

public interface WorkstationService {

    Workstation create(WorkstationDTO dto);

    Workstation update(Long id, WorkstationDTO dto);

    void delete(Long id);

    Workstation restore(Long id);

    Workstation findById(Long id);

    Workstation findByNo(String workstationNo);

    List<Workstation> findAll();

    List<Workstation> findInactive();

    List<Workstation> findByTeamId(Long teamId);

    void transfer(TransferDTO dto);

    List<WorkstationTransfer> getTransferHistory(Long workstationId);

    Double getLoadCapacityFromCache(String workstationNo);

    void cacheLoadCapacity(String workstationNo, Double loadCapacity);
}