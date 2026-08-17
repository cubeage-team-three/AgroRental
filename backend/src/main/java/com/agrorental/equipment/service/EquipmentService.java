package com.agrorental.equipment.service;

import com.agrorental.equipment.dto.EquipmentRequest;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final PartnerRepository partnerRepository;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            PartnerRepository partnerRepository) {

        this.equipmentRepository = equipmentRepository;
        this.partnerRepository = partnerRepository;
    }

    public Equipment addEquipment(EquipmentRequest request) {

        Partner partner = partnerRepository.findById(request.getPartnerId())
                .orElseThrow(() ->
                        new RuntimeException("Partner not found"));

        Equipment equipment = Equipment.builder()
                .name(request.getName())
                .category(request.getCategory())
                .brand(request.getBrand())
                .model(request.getModel())
                .manufacturingYear(request.getManufacturingYear())
                .capacity(request.getCapacity())
                .rentalPrice(request.getRentalPrice())
                .fuelType(request.getFuelType())
                .description(request.getDescription())
                .partner(partner)
                .locationAddress(request.getLocationAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .maintenanceNotes(request.getMaintenanceNotes())
                .build();

        return equipmentRepository.save(equipment);
    }

    public List<Equipment> getPartnerEquipment(Long partnerId) {

        return equipmentRepository.findByPartnerId(partnerId);
    }
}