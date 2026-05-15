package com.example.wex.client;

import com.example.wex.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TreasuryClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.fiscaldata.treasury.gov/services/api/fiscal_service")
            .build();

    public BigDecimal getExchangeRate(LocalDate date, String currency) {
        try {
            var url = String.format(
                    "/v1/accounting/od/rates_of_exchange?filter=record_date:gte:%s,record_date:lte:%s,currency:%s",
                    date, date, currency
            );

            var response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(TreasuryResponse.class)
                    .block();

            if (response == null || response.data().isEmpty()) {
                return null;
            }

            return new BigDecimal(response.data().get(0).exchange_rate());
        } catch (Exception ex) {
            throw new ValidationException("TREASURY_API_ERROR", "Failed to fetch exchange rate");
        }
    }

    public record TreasuryResponse(java.util.List<TreasuryRate> data) {}
    public record TreasuryRate(String currency, String exchange_rate, String record_date) {}
}
