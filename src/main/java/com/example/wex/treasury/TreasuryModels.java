package com.example.wex.treasury;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Minimal DTOs for the FiscalData "Treasury Reporting Rates of Exchange" endpoint.
 */
public final class TreasuryModels {
    private TreasuryModels() {}

    /** API envelope: { "data": [ ... ], "meta": { ... } } */
    public record FiscalDataResponse<T>(
            @JsonProperty("data") List<T> data,
            @JsonProperty("meta") Meta meta
    ) {
        public record Meta(
                @JsonProperty("count") Integer count,
                @JsonProperty("total-count") Integer totalCount,
                @JsonProperty("total-pages") Integer totalPages,
                @JsonProperty("page-number") Integer pageNumber,
                @JsonProperty("page-size") Integer pageSize
        ) {}
    }

    /**
     * Reference row used for in-memory catalog.
     * We use values exactly as downloaded (no mapping).
     */
    public record CurrencyRef(
            @JsonProperty("country_currency_desc") String countryCurrencyDesc,
            @JsonProperty("country") String country,
            @JsonProperty("currency") String currency
    ) {}

    /** Rate row used for conversion. */
    public record RateRow(
            @JsonProperty("country_currency_desc") String countryCurrencyDesc,
            @JsonProperty("record_date") String recordDate,
            @JsonProperty("exchange_rate") String exchangeRate
    ) {}
}