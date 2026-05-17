package com.example.wex.api;

import com.example.wex.model.*;
import com.example.wex.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PurchasesController implements PurchasesApi {

    private final PurchaseService service;

    @Override
    public ResponseEntity<PurchaseResponse> createPurchase(@Valid PurchaseCreateRequest request) {
        return ResponseEntity.status(201).body(service.createPurchase(request));
    }

    @Override
    public ResponseEntity<PurchaseConvertedResponse> getPurchaseConverted(UUID id, String currency) {
        return ResponseEntity.ok(service.getConvertedPurchase(id, currency));
    }
}

