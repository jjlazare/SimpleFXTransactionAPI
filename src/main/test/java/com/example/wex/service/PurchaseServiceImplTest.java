package com.example.wex.service;

import com.example.wex.entity.PurchaseEntity;
import com.example.wex.exception.NotFoundException;
import com.example.wex.exception.ValidationException;
import com.example.wex.repository.PurchaseRepository;
import com.example.wex.treasury.TreasuryRateClient;
import com.example.wex.validation.CurrencyDescriptorValidator;
import com.example.wex.validation.PurchaseValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseServiceImplTest {

    private PurchaseRepository repository;
    private CurrencyDescriptorValidator currencyValidator;
    private TreasuryRateClient rateClient;

    private PurchaseServiceImpl service;

    private UUID testId;

    @BeforeEach
    void setup() {
        repository = mock(PurchaseRepository.class);
        PurchaseValidator purchaseValidator = mock(PurchaseValidator.class);
        currencyValidator = mock(CurrencyDescriptorValidator.class);
        rateClient = mock(TreasuryRateClient.class);

        service = new PurchaseServiceImpl(
                purchaseValidator,
                currencyValidator,
                repository,
                new com.example.wex.mapper.PurchaseMapper(),
                rateClient
        );

        testId = UUID.randomUUID();
        PurchaseEntity entity = new PurchaseEntity();
        entity.setId(testId);
        entity.setDescription("Test");
        entity.setTransactionDate(LocalDate.now().minusDays(10));
        entity.setAmountUsd(new BigDecimal("100.00"));

        when(repository.findById(testId)).thenReturn(Optional.of(entity));
    }

    @Test
    void shouldConvertPurchaseSuccessfully() {
        when(rateClient.getRateWithinSixMonths(any(), any()))
                .thenReturn(new BigDecimal("1.5"));

        var result = service.getConvertedPurchase(testId, "Austria-Euro");

        assertEquals(150.0, result.getConvertedAmount());
        assertEquals("Austria-Euro", result.getTargetCurrency());
    }

    @Test
    void shouldThrowWhenUnsupportedCurrency() {
        doThrow(new ValidationException("UNSUPPORTED_CURRENCY", "bad"))
                .when(currencyValidator)
                .validateSupported(any());

        assertThrows(ValidationException.class,
                () -> service.getConvertedPurchase(testId, "BAD"));
    }

    @Test
    void shouldThrowWhenNoRateAvailable() {
        when(rateClient.getRateWithinSixMonths(any(), any()))
                .thenReturn(null);

        assertThrows(ValidationException.class,
                () -> service.getConvertedPurchase(testId, "Austria-Euro"));
    }

    @Test
    void shouldThrowWhenPurchaseNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.getConvertedPurchase(UUID.randomUUID(), "Austria-Euro"));
    }
}