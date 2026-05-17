package com.example.wex.treasury;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exposes the in-memory Treasury reference cache for visibility/testing.
 * Production note:
 * In a real system, this might be restricted or administratively scoped.
 */
@RestController
@RequestMapping("/api/v1/treasury")
public class TreasuryReferenceController {

    private final TreasuryReferenceCache cache;
    private final TreasuryReferenceService service;

    public TreasuryReferenceController(TreasuryReferenceCache cache, TreasuryReferenceService service) {
        this.cache = cache;
        this.service = service;
    }

    @PostMapping("/refresh-descriptions")
    public ResponseEntity<Void> refreshDescriptions() {
        service.refreshFromTreasury();
        return ResponseEntity.noContent().build();
    }

    /**
     * Optional: return full objects (country + currency + descriptor)
     */
    @GetMapping("/full")
    public ResponseEntity<List<TreasuryModels.CurrencyRef>> getAllFullReferences() {
        return ResponseEntity.ok(cache.list());
    }

    @GetMapping("/currency-descriptions")
    public ResponseEntity<List<String>> getAllCurrencyDescriptions() {
        List<String> result = cache.list().stream()
                .map(TreasuryModels.CurrencyRef::countryCurrencyDesc)
                .filter(s -> s != null && !s.isBlank())
                .sorted()
                .toList();
        return ResponseEntity.ok(result);
    }


}