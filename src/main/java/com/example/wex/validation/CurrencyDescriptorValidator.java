package com.example.wex.validation;

import com.example.wex.exception.ValidationException;
import com.example.wex.treasury.TreasuryReferenceCache;
import org.springframework.stereotype.Component;

/**
 * Validates that country_currency_desc values are available and supported.
 * No mapping: only exact downloaded values are allowed.
 */
@Component
public class CurrencyDescriptorValidator {

    private final TreasuryReferenceCache cache;

    public CurrencyDescriptorValidator(TreasuryReferenceCache cache) {
        this.cache = cache;
    }

    public void validateAvailable() {
        if (cache.isEmpty()) {
            throw new ValidationException(
                    "TREASURY_UNAVAILABLE",
                    "Currency reference data is unavailable. Please try again later."
            );
        }
    }

    public void validateSupported(String countryCurrencyDesc) {
        validateAvailable();
        if (countryCurrencyDesc == null || countryCurrencyDesc.isBlank()) {
            throw new ValidationException("VALIDATION_ERROR", "country_currency_desc is required");
        }
        if (!cache.supportsCountryCurrencyDesc(countryCurrencyDesc)) {
            throw new ValidationException(
                    "UNSUPPORTED_CURRENCY",
                    "Unsupported country_currency_desc: " + countryCurrencyDesc
            );
        }
    }
}