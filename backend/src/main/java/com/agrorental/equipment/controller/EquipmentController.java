package com.agrorental.equipment.controller;

import com.agrorental.equipment.dto.EquipmentRequest;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.service.EquipmentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    public ResponseEntity<Equipment> addEquipment(
            @Valid @RequestBody EquipmentRequest request) {

        Equipment equipment = equipmentService.addEquipment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(equipment);
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<List<Equipment>> getPartnerEquipment(
            @PathVariable Long partnerId) {

        return ResponseEntity.ok(
                equipmentService.getPartnerEquipment(partnerId)
        );
    }
}