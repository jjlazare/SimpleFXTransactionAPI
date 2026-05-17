package com.example.wex.validation;

import com.example.wex.exception.ValidationException;
import com.example.wex.treasury.TreasuryReferenceCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrencyDescriptorValidatorTest {

    @Test
    void shouldThrowWhenCacheEmpty() {
        var cache = mock(TreasuryReferenceCache.class);
        when(cache.isEmpty()).thenReturn(true);

        var validator = new CurrencyDescriptorValidator(cache);

        assertThrows(ValidationException.class,
                () -> validator.validateSupported("Austria-Euro"));
    }

    @Test
    void shouldThrowWhenUnsupported() {
        var cache = mock(TreasuryReferenceCache.class);

        when(cache.isEmpty()).thenReturn(false);
        when(cache.supportsCountryCurrencyDesc("BAD")).thenReturn(false);

        var validator = new CurrencyDescriptorValidator(cache);

        assertThrows(ValidationException.class,
                () -> validator.validateSupported("BAD"));
    }
}
