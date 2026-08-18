package com.agrorental.partner.controller;

import com.agrorental.partner.dto.PartnerDashboardResponse;
import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.service.PartnerService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partners")
@CrossOrigin(origins = "http://localhost:5173")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @PostMapping("/register")
    public ResponseEntity<Partner> registerPartner(
            @Valid @RequestBody PartnerRegistrationRequest request) {

        Partner partner = partnerService.registerPartner(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(partner);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerDashboardResponse> getPartnerDashboard(
            @PathVariable Long id) {

        return partnerService.getPartnerDashboard(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}