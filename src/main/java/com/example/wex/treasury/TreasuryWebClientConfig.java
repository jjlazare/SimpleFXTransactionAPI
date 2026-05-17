package com.example.wex.treasury;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Central WebClient config for Treasury FiscalData API.
 */
@Configuration
public class TreasuryWebClientConfig {

    @Bean
    public WebClient treasuryWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.fiscaldata.treasury.gov/services/api/fiscal_service")
                .build();
    }
}