package com.example.wex.treasury;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;

import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class TreasuryRateClientTest {

    private static WireMockServer wireMock;

    private TreasuryRateClient client;

    @BeforeAll
    static void start() {
        wireMock = new WireMockServer(8089);
        wireMock.start();
    }

    @AfterAll
    static void stop() {
        wireMock.stop();
    }

    @BeforeEach
    void setup() {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8089")
                .build();

        client = new TreasuryRateClient(webClient);
    }

    @Test
    void shouldReturnLatestRate() {

        wireMock.stubFor(get(urlPathMatching(".*rates_of_exchange.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "data": [
                            { "record_date": "2024-01-01", "exchange_rate": "1.1" },
                            { "record_date": "2024-02-01", "exchange_rate": "1.2" }
                          ]
                        }
                        """)));

        BigDecimal rate = client.getRateWithinSixMonths(
                LocalDate.of(2024, 3, 1),
                "Austria-Euro"
        );

        assertEquals(new BigDecimal("1.2"), rate);
    }
}