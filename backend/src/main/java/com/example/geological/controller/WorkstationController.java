package com.example.geological.controller;

import com.example.geological.dto.ResponseDTO;
import com.example.geological.dto.TransferDTO;
import com.example.geological.dto.WorkstationDTO;
import com.example.geological.entity.Workstation;
import com.example.geological.entity.WorkstationTransfer;
import com.example.geological.service.WorkstationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workstations")
@RequiredArgsConstructor
public class WorkstationController {

    private final WorkstationService workstationService;

    @GetMapping
    public ResponseDTO<List<WorkstationDTO>> getAll() {
        List<Workstation> workstations = workstationService.findAll();
        List<WorkstationDTO> dtos = workstations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    @GetMapping("/{id}")
    public ResponseDTO<WorkstationDTO> getById(@PathVariable Long id) {
        Workstation workstation = workstationService.findById(id);
        return ResponseDTO.success(convertToDTO(workstation));
    }

    @GetMapping("/by-no/{workstationNo}")
    public ResponseDTO<WorkstationDTO> getByNo(@PathVariable String workstationNo) {
        Workstation workstation = workstationService.findByNo(workstationNo);
        return ResponseDTO.success(convertToDTO(workstation));
    }

    @GetMapping("/by-team/{teamId}")
    public ResponseDTO<List<WorkstationDTO>> getByTeamId(@PathVariable Long teamId) {
        List<Workstation> workstations = workstationService.findByTeamId(teamId);
        List<WorkstationDTO> dtos = workstations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    @PostMapping
    public ResponseDTO<WorkstationDTO> create(@Valid @RequestBody WorkstationDTO dto) {
        Workstation workstation = workstationService.create(dto);
        return ResponseDTO.success("创建成功", convertToDTO(workstation));
    }

    @PutMapping("/{id}")
    public ResponseDTO<WorkstationDTO> update(@PathVariable Long id, @Valid @RequestBody WorkstationDTO dto) {
        Workstation workstation = workstationService.update(id, dto);
        return ResponseDTO.success("更新成功", convertToDTO(workstation));
    }

    @DeleteMapping("/{id}")
    public ResponseDTO<Void> delete(@PathVariable Long id) {
        workstationService.delete(id);
        return ResponseDTO.success(null);
    }

    @PostMapping("/transfer")
    public ResponseDTO<Void> transfer(@Valid @RequestBody TransferDTO dto) {
        workstationService.transfer(dto);
        return ResponseDTO.success("转移成功", null);
    }

    @GetMapping("/{id}/transfer-history")
    public ResponseDTO<List<WorkstationTransfer>> getTransferHistory(@PathVariable Long id) {
        List<WorkstationTransfer> history = workstationService.getTransferHistory(id);
        return ResponseDTO.success(history);
    }

    @GetMapping("/{workstationNo}/load-capacity")
    public ResponseDTO<Double> getLoadCapacity(@PathVariable String workstationNo) {
        Double loadCapacity = workstationService.getLoadCapacityFromCache(workstationNo);
        return ResponseDTO.success(loadCapacity);
    }

    private WorkstationDTO convertToDTO(Workstation workstation) {
        WorkstationDTO dto = new WorkstationDTO();
        dto.setId(workstation.getId());
        dto.setWorkstationNo(workstation.getWorkstationNo());
        dto.setLoadCapacity(workstation.getLoadCapacity() != null ? workstation.getLoadCapacity().toString() : null);
        dto.setWorkstationType(workstation.getWorkstationType());
        dto.setAdaptStation(workstation.getAdaptStation());
        if (workstation.getCurrentTeam() != null) {
            dto.setCurrentTeamId(workstation.getCurrentTeam().getId());
            dto.setCurrentTeamName(workstation.getCurrentTeam().getTeamName());
        }
        return dto;
    }
}