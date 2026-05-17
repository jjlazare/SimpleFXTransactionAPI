package com.example.wex.treasury;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TreasuryReferenceLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TreasuryReferenceLoader.class);

    private final TreasuryReferenceService service;

    public TreasuryReferenceLoader(TreasuryReferenceService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        int fromDb = service.loadFromDbToCache();
        if (fromDb > 0) {
            log.warn("Loaded {} currency descriptions from DB snapshot (may be stale). Will attempt Treasury refresh.", fromDb);
        } else {
            log.warn("No currency descriptions found in DB snapshot. Will attempt Treasury refresh.");
        }

        try {
            int refreshed = service.refreshFromTreasury();
        } catch (Exception ex) {
            // If DB had data, keep operating. If DB empty, remain “downed”
            if (fromDb > 0) {
                log.warn("Treasury refresh failed at startup; continuing with DB snapshot of {} rows.", fromDb);
            } else {
                log.error("Treasury refresh failed and DB snapshot empty; conversion endpoints will be unavailable until refresh succeeds.");
            }
        }
    }
}