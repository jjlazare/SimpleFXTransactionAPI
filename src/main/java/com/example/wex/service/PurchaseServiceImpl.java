package com.example.wex.service;

import com.example.wex.entity.PurchaseEntity;
import com.example.wex.exception.NotFoundException;
import com.example.wex.exception.ValidationException;
import com.example.wex.mapper.PurchaseMapper;
import com.example.wex.model.PurchaseCreateRequest;
import com.example.wex.model.PurchaseConvertedResponse;
import com.example.wex.model.PurchaseResponse;
import com.example.wex.repository.PurchaseRepository;
import com.example.wex.treasury.TreasuryRateClient;
import com.example.wex.validation.CurrencyDescriptorValidator;
import com.example.wex.validation.PurchaseValidator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseValidator purchaseValidator;
    private final CurrencyDescriptorValidator currencyDescriptorValidator;
    private final PurchaseRepository repository;
    private final PurchaseMapper mapper;
    private final TreasuryRateClient treasuryRateClient;

    @Override
    public PurchaseResponse createPurchase(PurchaseCreateRequest request) {
        purchaseValidator.validate(request);
        var entity = mapper.toEntity(request);
        var saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    public PurchaseConvertedResponse getConvertedPurchase(UUID id, String countryCurrencyDesc) {
        log.debug("::getConvertedPurchase:: id:{} , CountryCurrencyDesc{}", id, countryCurrencyDesc);

        var entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("PURCHASE_NOT_FOUND", "Purchase not found"));

        // No mapping: must be a Treasury-downloaded country_currency_desc.
        currencyDescriptorValidator.validateSupported(countryCurrencyDesc);

        BigDecimal rate = treasuryRateClient.getRateWithinSixMonths(entity.getTransactionDate(), countryCurrencyDesc);

        if (rate == null) {
            throw new ValidationException(
                    "NO_RATE_AVAILABLE",
                    "No exchange rate available within 6 months on or before the purchase date for " + countryCurrencyDesc
            );
        }


        log.debug("::getConvertedPurchase:: + calling getPurchaseConvertedResponse");
        return getPurchaseConvertedResponse(countryCurrencyDesc, entity, rate);
    }

    private static @NonNull PurchaseConvertedResponse getPurchaseConvertedResponse(String countryCurrencyDesc, PurchaseEntity entity, BigDecimal rate) {
        log.debug("::getPurchaseConvertedResponse:: countryCurrencyDesc{}, entity{}, rate{}", countryCurrencyDesc, entity, rate);
        BigDecimal converted = entity.getAmountUsd().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        log.debug("Converted amount: {}", converted);

        PurchaseConvertedResponse resp = new PurchaseConvertedResponse();
        resp.setId(entity.getId());
        resp.setDescription(entity.getDescription());
        resp.setTransactionDate(entity.getTransactionDate());

        resp.setAmountUsd(
                entity.getAmountUsd() != null
                        ? entity.getAmountUsd().setScale(2, RoundingMode.HALF_UP).toPlainString()
                        : null
        );

        log.debug("Amount rounded set: {}", resp.getAmountUsd());

        resp.setTargetCurrency(countryCurrencyDesc);

        resp.setExchangeRateUsed(rate.doubleValue());
        resp.setConvertedAmount(converted.doubleValue());
        return resp;
    }
}