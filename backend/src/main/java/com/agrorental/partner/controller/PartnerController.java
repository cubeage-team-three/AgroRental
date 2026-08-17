package com.agrorental.partner.controller;

import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.service.PartnerService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}