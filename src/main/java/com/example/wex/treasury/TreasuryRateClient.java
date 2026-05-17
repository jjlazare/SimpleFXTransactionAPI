package com.example.wex.treasury;

import com.example.wex.exception.ValidationException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Function;

/**
 * Fetches exchange rates for a given country_currency_desc using Treasury FiscalData.
 */
@Slf4j
@Component
public class TreasuryRateClient {

    private static final ParameterizedTypeReference<
            TreasuryModels.FiscalDataResponse<TreasuryModels.RateRow>
            > RATE_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private final WebClient webClient;

    public TreasuryRateClient(WebClient treasuryWebClient) {
        this.webClient = treasuryWebClient;
    }

    /**
     * Returns the exchange rate for the latest record_date that is:
     * - within 6 months of purchaseDate AND
     * - <= purchaseDate
     * Returns null when no qualifying rate exists.
     */
    public BigDecimal getRateWithinSixMonths(LocalDate purchaseDate, String countryCurrencyDesc) {
        log.debug("::getRateWithinSixMonths::");
        Function<UriBuilder, URI> uri = getUriBuilderURIFunction(purchaseDate, countryCurrencyDesc);

        try {
            // We are using WebClient in an otherwise synchronous (Spring MVC) application.
            // Calling .block() directly on the request thread can lead to thread starvation
            // under load if the external Treasury API is slow or unresponsive.
            //
            // To mitigate this, we wrap the blocking call in a reactive Mono and offload it
            // to Reactor's boundedElastic scheduler. This ensures the blocking I/O work is
            // handled by a dedicated thread pool rather than consuming servlet request threads.
            //
            // This preserves our synchronous service design while improving resilience and
            // preventing potential performance degradation under load.

            TreasuryModels.FiscalDataResponse<TreasuryModels.RateRow> resp =
                    Mono.fromCallable(() ->
                                    webClient.get()
                                            .uri(uri)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .retrieve()
                                            .bodyToMono(RATE_RESPONSE_TYPE)
                                            .timeout(java.time.Duration.ofSeconds(3))
                                            .retry(1)
                                            .block()
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .block();

            if (resp == null || resp.data() == null || resp.data().isEmpty()) {
                return null;
            }

            // Robust selection: find latest parsable date with valid rate
            LocalDate minDate = purchaseDate.minusMonths(6);
            TreasuryModels.RateRow best = resp.data().stream()
                    .filter(r -> r.recordDate() != null && r.exchangeRate() != null && !r.exchangeRate().isBlank())
                    .map(r -> {
                        try {
                            return new java.util.AbstractMap.SimpleEntry<>(LocalDate.parse(r.recordDate()), r);
                        } catch (Exception ignored) {
                            return null; // skip bad dates
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(e -> !e.getKey().isAfter(purchaseDate)) // record_date <= purchaseDate
                    .filter(e -> !e.getKey().isBefore(minDate))      // record_date >= purchaseDate - 6 months

                    .max(java.util.Map.Entry.comparingByKey())
                    .map(java.util.Map.Entry::getValue)
                    .orElse(null);

            if (best == null) {
                log.warn("NO RATE WITH VALID DATE FOUND. getRateWithinSixMonths is returning null.");
                return null;
            }

            try {
                log.debug("Best exchange rate selected: " + best.exchangeRate());
                return new BigDecimal(best.exchangeRate());
            } catch (NumberFormatException ex) {
                return null; // skip/none usable
            }

        } catch (Exception ex) {
            throw new ValidationException("TREASURY_API_ERROR", "Failed to fetch exchange rate");
        }
    }

    private static @NonNull Function<UriBuilder, URI> getUriBuilderURIFunction(LocalDate purchaseDate, String countryCurrencyDesc) {
        log.debug("::getUriBuilderURIFunction::");

        if (purchaseDate == null) {
            throw new ValidationException("VALIDATION_ERROR", "Transaction date is required");
        }
        if (countryCurrencyDesc == null || countryCurrencyDesc.isBlank()) {
            throw new ValidationException("VALIDATION_ERROR", "country_currency_desc is required");
        }

        LocalDate minDate = purchaseDate.minusMonths(6);
        log.debug("Min date set to purchasedate minus 6 months: purchaseDate{} dateMinus6months{}", purchaseDate, minDate);

        return uriBuilder -> uriBuilder
                .path("/v1/accounting/od/rates_of_exchange")
                .queryParam("fields", "country_currency_desc,exchange_rate,record_date")
                .queryParam("filter", String.format(
                        "record_date:gte:%s,record_date:lte:%s,country_currency_desc:eq:%s",
                        minDate, purchaseDate, countryCurrencyDesc
                ))
                .queryParam("sort", "-record_date")
                .queryParam("page[size]", 1000)
                .build();
    }
}