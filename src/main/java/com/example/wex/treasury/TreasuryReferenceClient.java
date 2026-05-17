package com.example.wex.treasury;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Calls the FiscalData API to download the catalog of supported country-currency descriptors.
 */
@Slf4j
@Component
public class TreasuryReferenceClient {

    private static final ParameterizedTypeReference<
            TreasuryModels.FiscalDataResponse<TreasuryModels.CurrencyRef>
            > REF_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private final WebClient webClient;

    public TreasuryReferenceClient(WebClient treasuryWebClient) {
        this.webClient = treasuryWebClient;
    }

    /**
     * Download reference rows (country_currency_desc,country,currency) using pagination.
     * Values are used exactly as returned (no mapping).
     */
    public List<TreasuryModels.CurrencyRef> downloadAllCurrencyRefs() {
        final int pageSize = 1000;
        int pageNumber = 1;

        List<TreasuryModels.CurrencyRef> all = new ArrayList<>();

        while (true) {
            final int pn = pageNumber;

            log.debug("Fetching currency refs page {}", pn);

            Function<UriBuilder, URI> uri = uriBuilder -> uriBuilder
                    .path("/v1/accounting/od/rates_of_exchange")
                    .queryParam("fields", "country_currency_desc,country,currency")
                    .queryParam("page[size]", pageSize)
                    .queryParam("page[number]", pn)
                    .build();

            TreasuryModels.FiscalDataResponse<TreasuryModels.CurrencyRef> resp = webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(REF_RESPONSE_TYPE)
                    .timeout(java.time.Duration.ofSeconds(3))
                    .retry(1)
                    .blockOptional()
                    .orElse(null);

            if (resp == null || resp.data() == null || resp.data().isEmpty()) {
                break;
            }

            all.addAll(resp.data());
            log.debug("Fetched {} records (total so far: {})", resp.data().size(), all.size());

            if (resp.data().size() < pageSize) {
                break;
            }

            pageNumber++;
            if (pageNumber > 500) break; // safety stop
        }

        return all;
    }
}