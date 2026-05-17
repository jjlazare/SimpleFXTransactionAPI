package com.example.wex.treasury;

import com.example.wex.entity.CurrencyRefEntity;
import com.example.wex.exception.ValidationException;
import com.example.wex.repository.CurrencyRefRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class TreasuryReferenceService {

    private final TreasuryReferenceClient treasuryClient;
    private final TreasuryReferenceCache cache;
    private final CurrencyRefRepository repo;

    public TreasuryReferenceService(TreasuryReferenceClient treasuryClient,
                                    TreasuryReferenceCache cache,
                                    CurrencyRefRepository repo) {
        this.treasuryClient = treasuryClient;
        this.cache = cache;
        this.repo = repo;
    }

    /** Load from DB into in-memory cache. Returns number loaded. */
    public int loadFromDbToCache() {
        List<CurrencyRefEntity> rows = repo.findAllOrdered();
        List<TreasuryModels.CurrencyRef> refs = rows.stream()
                .map(r -> new TreasuryModels.CurrencyRef(
                        r.getCountryCurrencyDesc(),
                        r.getCountry(),
                        r.getCurrency()
                ))
                .toList();

        cache.replaceAll(refs);
        return refs.size();
    }

    /**
     * Download from Treasury, update cache, persist to DB.
     * Only replaces cache after successful download.
     */
    @Transactional
    public int refreshFromTreasury() {
        List<TreasuryModels.CurrencyRef> refs;
        try {
            refs = treasuryClient.downloadAllCurrencyRefs();
        } catch (Exception ex) {
            // keep existing cache intact
            log.warn("Treasury reference refresh failed; keeping existing cache", ex);
            throw new ValidationException("TREASURY_API_ERROR", "Failed to refresh currency reference data");
        }

        if (refs == null || refs.isEmpty()) {
            log.warn("Treasury reference refresh returned empty set; keeping existing cache");
            throw new ValidationException("TREASURY_API_ERROR", "Treasury reference data was empty");
        }

        // Persist snapshot (replace DB contents)
        Instant now = Instant.now();
        repo.deleteAllInBatch();
        repo.flush();

        List<CurrencyRefEntity> entities = refs.stream()
                .filter(r -> r != null && r.countryCurrencyDesc() != null && !r.countryCurrencyDesc().isBlank())
                .map(r -> {
                    CurrencyRefEntity e = new CurrencyRefEntity();
                    e.setCountryCurrencyDesc(r.countryCurrencyDesc());
                    e.setCountry(r.country());
                    e.setCurrency(r.currency());
                    e.setLastUpdated(now);
                    return e;
                })
                .toList();

        repo.saveAll(entities);

        // Update cache last (so DB write is the “commit point”)
        cache.replaceAll(refs);

        log.info("Treasury reference refresh from live API succeeded: {} rows", entities.size());
        return entities.size();
    }
}