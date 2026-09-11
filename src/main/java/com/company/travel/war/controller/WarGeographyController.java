package com.company.travel.war.controller;

import com.company.travel.war.dto.CreateWarGeographyRequest;
import com.company.travel.war.dto.WarGeographyResponse;
import com.company.travel.war.service.WarGeographyService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/war-geographies")
public class WarGeographyController {

    private final WarGeographyService service;

    public WarGeographyController(WarGeographyService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WAR_GEOGRAPHY_MAINTAIN')")
    public ResponseEntity<WarGeographyResponse> create(
            @Valid @RequestBody CreateWarGeographyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WarGeographyResponse.from(service.create(request)));
    }
}