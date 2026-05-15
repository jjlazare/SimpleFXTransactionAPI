package com.example.wex.service;

import com.example.wex.dto.PurchaseCreateDto;
import com.example.wex.model.PurchaseCreateRequest;
import com.example.wex.model.PurchaseConvertedResponse;
import com.example.wex.model.PurchaseResponse;

import java.util.UUID;

public interface PurchaseService {

    PurchaseResponse createPurchase(PurchaseCreateRequest request);
    PurchaseConvertedResponse getConvertedPurchase(UUID id, String currency);
}
