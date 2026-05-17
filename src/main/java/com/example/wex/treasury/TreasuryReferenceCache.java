package com.example.wex.treasury;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory catalog of country-currency descriptors downloaded from Treasury FiscalData.
 * No mapping: values are used exactly as downloaded.
 */
@Slf4j
@Component
public class TreasuryReferenceCache {

    private final List<TreasuryModels.CurrencyRef> orderedRefs = new ArrayList<>();
    private final Set<String> descSet = ConcurrentHashMap.newKeySet();

    /** Replace the cache contents atomically. */
    public synchronized void replaceAll(List<TreasuryModels.CurrencyRef> refs) {
        log.debug("::replaceAll:: Clearing cache contents");
        orderedRefs.clear();
        descSet.clear();
        if (refs == null) return;

        for (var r : refs) {
            if (r == null) continue;
            if (r.countryCurrencyDesc() == null || r.countryCurrencyDesc().isBlank()) continue;
            orderedRefs.add(r);
            descSet.add(r.countryCurrencyDesc());
        }
    }

    /** Stable list for index-based selection in tests. */
    public List<TreasuryModels.CurrencyRef> list() {
        return Collections.unmodifiableList(orderedRefs);
    }

    public boolean isEmpty() {
        return orderedRefs.isEmpty();
    }

    public boolean supportsCountryCurrencyDesc(String desc) {
        return desc != null && descSet.contains(desc);
    }

    /** Pick by index (wrap around) to keep tests simple and deterministic. */
    public Optional<TreasuryModels.CurrencyRef> pickByIndex(int index) {
        log.debug("::pickByIndex::");
        if (orderedRefs.isEmpty()) return Optional.empty();
        int i = Math.floorMod(index, orderedRefs.size());

        log.debug("Returning: {}", orderedRefs.get(i));
        return Optional.ofNullable(orderedRefs.get(i));
    }
}