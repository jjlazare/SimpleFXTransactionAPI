package com.example.wex.service;

import com.example.wex.exception.NotFoundException;
import com.example.wex.exception.ValidationException;
import com.example.wex.mapper.PurchaseMapper;
import com.example.wex.model.PurchaseCreateRequest;
import com.example.wex.model.PurchaseConvertedResponse;
import com.example.wex.model.PurchaseResponse;
import com.example.wex.repository.PurchaseRepository;
import com.example.wex.validation.PurchaseValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.wex.client.TreasuryClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseValidator validator;
    private final PurchaseRepository repository;
    private final PurchaseMapper mapper;
    private final TreasuryClient treasuryClient;

    @Override
    public PurchaseResponse createPurchase(PurchaseCreateRequest request) {

        validator.validate(request);

        var entity = mapper.toEntity(request);
        var saved = repository.save(entity);

        return mapper.toResponse(saved);
    }

    @Override
    public PurchaseConvertedResponse getConvertedPurchase(UUID id, String currency) {

        var entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("PURCHASE_NOT_FOUND", "Purchase not found"));

        var rate = treasuryClient.getExchangeRate(entity.getTransactionDate(), currency);

        if (rate == null) {
            throw new ValidationException("NO_RATE_AVAILABLE", "No exchange rate available for " + currency);
        }

        var converted = entity.getAmountUsd().multiply(rate);

        var resp = new PurchaseConvertedResponse();
        resp.setId(entity.getId());
        resp.setDescription(entity.getDescription());
        resp.setTransactionDate(entity.getTransactionDate());
        resp.setAmountUsd(entity.getAmountUsd());
        resp.setTargetCurrency(currency);
        resp.setExchangeRateUsed(rate);
        resp.setConvertedAmount(converted.setScale(2, java.math.RoundingMode.HALF_UP));

        return resp;
    }
}

