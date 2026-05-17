package com.example.wex.treasury;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TreasuryReferenceRefresher {

    private final TreasuryReferenceService service;

    public TreasuryReferenceRefresher(TreasuryReferenceService service) {
        this.service = service;
    }


    @Scheduled(
            initialDelayString = "${treasury.reference.refresh.ms}",
            fixedDelayString = "${treasury.reference.refresh.ms}"
    )
    public void refresh() {
        long delay = Long.parseLong(System.getProperty("treasury.reference.refresh.ms", "900000"));
        if (delay <= 0) return; // simple disable if 0

        try {
            service.refreshFromTreasury();
        } catch (Exception ex) {
            log.warn("Scheduled Treasury reference refresh failed; keeping existing cache", ex);
        }
    }
}
