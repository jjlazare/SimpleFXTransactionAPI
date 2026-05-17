package com.example.wex.treasury;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreasuryReferenceCacheTest {

    @Test
    void shouldStoreAndRetrieveValues() {
        TreasuryReferenceCache cache = new TreasuryReferenceCache();

        var refs = List.of(
                new TreasuryModels.CurrencyRef("Austria-Euro", "Austria", "Euro"),
                new TreasuryModels.CurrencyRef("Canada-Dollar", "Canada", "Dollar")
        );

        cache.replaceAll(refs);

        assertFalse(cache.isEmpty());
        assertTrue(cache.supportsCountryCurrencyDesc("Austria-Euro"));
    }

    @Test
    void pickByIndexShouldWrapAround() {
        TreasuryReferenceCache cache = new TreasuryReferenceCache();

        cache.replaceAll(List.of(
                new TreasuryModels.CurrencyRef("A", "A", "A"),
                new TreasuryModels.CurrencyRef("B", "B", "B")
        ));

        var result = cache.pickByIndex(3).get(); // wraps

        assertEquals("B", result.countryCurrencyDesc());
    }
}
