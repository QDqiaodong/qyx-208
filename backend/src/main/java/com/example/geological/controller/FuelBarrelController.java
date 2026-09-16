package com.example.geological.controller;

import com.example.geological.dto.FuelBarrelDTO;
import com.example.geological.dto.ResponseDTO;
import com.example.geological.entity.FuelBarrel;
import com.example.geological.service.FuelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 柴油桶档案：桶号、额定升数、当前余量。
 * 油桶独立建档，不属于小队操作台/样品袋资产，不碰小队承重等任何资产列。
 */
@RestController
@RequestMapping("/api/fuel-barrels")
@RequiredArgsConstructor
public class FuelBarrelController {

    private final FuelService fuelService;

    @GetMapping
    public ResponseDTO<List<FuelBarrelDTO>> getAll() {
        List<FuelBarrel> barrels = fuelService.findAllBarrels();
        return ResponseDTO.success(barrels.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseDTO<FuelBarrelDTO> getById(@PathVariable Long id) {
        return ResponseDTO.success(toDTO(fuelService.findBarrelById(id)));
    }

    @PostMapping
    public ResponseDTO<FuelBarrelDTO> create(@Valid @RequestBody FuelBarrelDTO dto) {
        FuelBarrel barrel = fuelService.createBarrel(dto);
        return ResponseDTO.success("油桶建档成功", toDTO(barrel));
    }

    private FuelBarrelDTO toDTO(FuelBarrel barrel) {
        FuelBarrelDTO dto = new FuelBarrelDTO();
        dto.setId(barrel.getId());
        dto.setBarrelNo(barrel.getBarrelNo());
        dto.setRatedCapacity(barrel.getRatedCapacity());
        dto.setCurrentLevel(barrel.getCurrentLevel());
        dto.setRemark(barrel.getRemark());
        return dto;
    }
}
