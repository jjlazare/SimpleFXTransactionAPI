package com.example.wex.validation;

import com.example.wex.model.PurchaseCreateRequest;
import com.example.wex.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PurchaseValidator {

    public void validate(PurchaseCreateRequest req) {

        if (req.getDescription() == null || req.getDescription().isBlank()) {
            throw new ValidationException("VALIDATION_ERROR", "Description is required");
        }

        if (req.getDescription().length() > 50) {
            throw new ValidationException("VALIDATION_ERROR", "Description must be <= 50 characters");
        }

        if (req.getTransactionDate() == null) {
            throw new ValidationException("VALIDATION_ERROR", "Transaction date is required");
        }

        if (req.getTransactionDate().isAfter(LocalDate.now())) {
            throw new ValidationException("VALIDATION_ERROR", "Transaction date cannot be in the future");
        }

        if (req.getAmountUsd() == null) {
            throw new ValidationException("VALIDATION_ERROR", "Amount is required");
        }

        if (req.getAmountUsd().doubleValue() < 0.01) {
            throw new ValidationException("VALIDATION_ERROR", "Amount must be at least 0.01");
        }
    }
}
