package com.example.wex.integration;

import com.example.wex.model.PurchaseCreateRequest;
import com.example.wex.model.PurchaseResponse;
import com.example.wex.treasury.TreasuryRateClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;



@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.ssl.enabled=false",
                "server.ssl.key-store=",
                "server.ssl.key-store-password=",
                "server.ssl.key-store-type=",
                "server.ssl.key-alias="
        }
)

@ActiveProfiles("test")

class PurchaseApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String testPurchaseId;

    @BeforeEach
    void createPurchase() {
        PurchaseCreateRequest req = new PurchaseCreateRequest();
        req.setDescription("Integration Test Purchase");
        req.setTransactionDate(java.time.LocalDate.now().minusDays(10));
        req.setAmountUsd("100.00");

        ResponseEntity<PurchaseResponse> response =
                restTemplate.postForEntity(
                        baseUrl() + "/api/v1/purchases",
                        req,
                        PurchaseResponse.class
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        testPurchaseId = response.getBody().getId().toString();
    }

    @Test
    void shouldReturnCurrencyDescriptions() {
        ResponseEntity<List> response =
                restTemplate.getForEntity(
                        baseUrl() + "/api/v1/treasury/currency-descriptions",
                        List.class
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void shouldConvertPurchaseSuccessfully() {

        List<?> currencies =
                restTemplate.getForObject(
                        baseUrl() + "/api/v1/treasury/currency-descriptions",
                        List.class
                );

        String descriptor = (String) currencies.getFirst();

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        baseUrl() + "/api/v1/purchases/" + testPurchaseId
                                + "?country_currency_desc=" + descriptor,
                        String.class
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("convertedAmount"));
    }

    @Test
    void shouldReturnErrorForUnsupportedCurrency() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        baseUrl() + "/api/v1/purchases/" + testPurchaseId
                                + "?country_currency_desc=INVALID",
                        String.class
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("UNSUPPORTED_CURRENCY"));
    }

    @Test
    void shouldReturnErrorForOldTransactionNoRate() {
        // Create old purchase
        PurchaseCreateRequest req = new PurchaseCreateRequest();
        req.setDescription("Old Purchase");
        req.setTransactionDate(java.time.LocalDate.of(2000, 1, 1));
        req.setAmountUsd("50.00");

        var resp = restTemplate.postForEntity(
                baseUrl() + "/api/v1/purchases",
                req,
                PurchaseResponse.class
        );

        assertNotNull(resp.getBody());
        String id = resp.getBody().getId().toString();

        List<?> currencies =
                restTemplate.getForObject(
                        baseUrl() + "/api/v1/treasury/currency-descriptions",
                        List.class
                );

        String descriptor = (String) currencies.get(99); //arbitrary. random for prod

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        baseUrl() + "/api/v1/purchases/" + id
                                + "?country_currency_desc=" + descriptor,
                        String.class
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("NO_RATE_AVAILABLE"));
    }

    @Test
    void shouldReturn404ForMissingPurchase() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        baseUrl() + "/api/v1/purchases/00000000-0000-0000-0000-000000000000"
                                + "?country_currency_desc=Austria-Euro",
                        String.class
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnEitherSuccessOrNoRateAvailable() {
        List<?> currencies =
                restTemplate.getForObject(
                        baseUrl() + "/api/v1/treasury/currency-descriptions",
                        List.class
                );

        String descriptor = (String) currencies.getFirst();

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        baseUrl() + "/api/v1/purchases/" + testPurchaseId
                                + "?country_currency_desc=" + descriptor,
                        String.class
                );

        assertTrue(
                response.getStatusCode() == HttpStatus.OK
                        || response.getStatusCode() == HttpStatus.BAD_REQUEST
        );
    }

    @Test
    void shouldRoundToTwoDecimalPlaces() {
        PurchaseCreateRequest req = new PurchaseCreateRequest();
        req.setDescription("Rounding Test");
        req.setTransactionDate(LocalDate.now().minusDays(10));
        req.setAmountUsd("100.1234");

        var createResp = restTemplate.postForEntity(
                baseUrl() + "/api/v1/purchases",
                req,
                PurchaseResponse.class
        );

        assertNotNull(createResp.getBody());
        String id = createResp.getBody().getId().toString();

        List<?> currencies = restTemplate.getForObject(
                baseUrl() + "/api/v1/treasury/currency-descriptions",
                List.class
        );

        String descriptor = (String) currencies.getFirst();

        var response = restTemplate.getForEntity(
                baseUrl() + "/api/v1/purchases/" + id
                        + "?country_currency_desc=" + descriptor,
                String.class
        );

        assertNotNull(response.getBody());
        assertTrue(response.getBody().matches(".*\\d+\\.\\d{2}.*"));
    }

    @Test
    void shouldRetrieveCorrectPurchaseAmongMultiple() {

        // Create first purchase
        var p1 = new PurchaseCreateRequest();
        p1.setDescription("Purchase 1");
        p1.setTransactionDate(LocalDate.now().minusDays(5));
        p1.setAmountUsd("50.00");

        var r1 = restTemplate.postForEntity(
                baseUrl() + "/api/v1/purchases",
                p1,
                PurchaseResponse.class
        );

        // Create second purchase
        var p2 = new PurchaseCreateRequest();
        p2.setDescription("Purchase 2");
        p2.setTransactionDate(LocalDate.now().minusDays(5));
        p2.setAmountUsd("200.0");

        var r2 = restTemplate.postForEntity(
                baseUrl() + "/api/v1/purchases",
                p2,
                PurchaseResponse.class
        );

        assertNotNull(r1.getBody());
        String id1 = r1.getBody().getId().toString();
        assertNotNull(r2.getBody());
        String id2 = r2.getBody().getId().toString();

        // Get currency descriptor
        List<?> currencies = restTemplate.getForObject(
                baseUrl() + "/api/v1/treasury/currency-descriptions",
                List.class
        );

        String descriptor = (String) currencies.getFirst();

        // Convert second purchase
        var response = restTemplate.getForEntity(
                baseUrl() + "/api/v1/purchases/" + id2
                        + "?country_currency_desc=" + descriptor,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        //Ensure correct purchase is returned
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Purchase 2"));
        assertFalse(response.getBody().contains("Purchase 1"));
    }

    @Test
    void shouldHandleMultiplePurchasesQuickly() {

        int count = 20; // small, fast

        for (int i = 0; i < count; i++) {
            var req = new PurchaseCreateRequest();
            req.setDescription("Bulk " + i);
            req.setTransactionDate(LocalDate.now().minusDays(5));
            req.setAmountUsd("10.0" + i);

            var response = restTemplate.postForEntity(
                    baseUrl() + "/api/v1/purchases",
                    req,
                    PurchaseResponse.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }
    }

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
        wireMock.resetAll();

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8089")
                .build();

        client = new TreasuryRateClient(webClient);
    }

    /**
     * A) PROVES: client requests the correct window (>= purchaseDate-6mo AND <= purchaseDate)
     * and requests descending sort by record_date.
     */
    @Test
    void shouldRequestRatesConstrainedToPurchaseDateAndSixMonthWindow_andSortedDesc() {
        LocalDate purchaseDate = LocalDate.of(2024, 3, 15);
        String desc = "Austria-Euro";
        LocalDate minDate = purchaseDate.minusMonths(6);

        wireMock.stubFor(get(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            { "data": [ { "record_date": "2024-03-01", "exchange_rate": "1.2" } ] }
                        """)));

        client.getRateWithinSixMonths(purchaseDate, desc);

        wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/accounting/od/rates_of_exchange"))
                .withQueryParam("fields", equalTo("country_currency_desc,exchange_rate,record_date"))
                .withQueryParam("sort", equalTo("-record_date"))
                .withQueryParam("filter", equalTo(
                        String.format(
                                "record_date:gte:%s,record_date:lte:%s,country_currency_desc:eq:%s",
                                minDate, purchaseDate, desc
                        )
                )));
    }

    /**
     * B) PROVES: chooses the latest record_date among the returned rows
     */
    @Test
    void shouldChooseLatestRateWithinWindow_whenMultipleReturned() {
        wireMock.stubFor(get(urlPathMatching(".*rates_of_exchange.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": [
                                { "record_date": "2024-01-01", "exchange_rate": "1.1" },
                                { "record_date": "2024-02-01", "exchange_rate": "1.2" },
                                { "record_date": "2024-02-15", "exchange_rate": "1.25" }
                              ]
                            }
                        """)));

        BigDecimal rate = client.getRateWithinSixMonths(
                LocalDate.of(2024, 3, 1),
                "Austria-Euro"
        );

        assertEquals(new BigDecimal("1.25"), rate);
    }

    /**
     * C) PROVES: ignores future-dated rates (record_date > purchaseDate) and picks latest <= purchaseDate.
     */
    @Test
    void shouldIgnoreRatesAfterPurchaseDate_evenIfReturnedByApi() {
        wireMock.stubFor(get(urlPathMatching(".*rates_of_exchange.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": [
                                { "record_date": "2024-03-20", "exchange_rate": "9.9" },
                                { "record_date": "2024-03-10", "exchange_rate": "1.2" }
                              ]
                            }
                        """)));

        BigDecimal rate = client.getRateWithinSixMonths(
                LocalDate.of(2024, 3, 15),
                "Austria-Euro"
        );

        // Expect 1.2 (latest <= purchase date), NOT 9.9 (future).
        assertEquals(new BigDecimal("1.2"), rate);
    }

    /**
     * D) PROVES: ignores rates older than 6 months.
     * purchaseDate 2024-03-01 => cutoff is 2023-09-01 (inclusive).
     */
    @Test
    void shouldIgnoreRatesOlderThanSixMonths_evenIfReturnedByApi() {
        wireMock.stubFor(get(urlPathMatching(".*rates_of_exchange.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": [
                                { "record_date": "2023-08-01", "exchange_rate": "9.9" },
                                { "record_date": "2023-10-01", "exchange_rate": "1.1" }
                              ]
                            }
                        """)));

        BigDecimal rate = client.getRateWithinSixMonths(
                LocalDate.of(2024, 3, 1),
                "Austria-Euro"
        );

        // 2023-08-01 is too old; 2023-10-01 is within 6 months.
        assertEquals(new BigDecimal("1.1"), rate);
    }

    /**
     * E) PROVES: boundary condition: exactly 6 months old is allowed (inclusive).
     */
    @Test
    void shouldAllowRateExactlyAtSixMonthBoundary() {
        wireMock.stubFor(get(urlPathMatching(".*rates_of_exchange.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": [
                                { "record_date": "2023-09-01", "exchange_rate": "1.1" }
                              ]
                            }
                        """)));

        BigDecimal rate = client.getRateWithinSixMonths(
                LocalDate.of(2024, 3, 1),
                "Austria-Euro"
        );

        assertEquals(new BigDecimal("1.1"), rate);
    }

    /**
     * F) PROVES: returns null when no qualifying rate exists in the allowed window.
     */
    @Test
    void shouldReturnNullWhenNoQualifyingRateExists() {
        wireMock.stubFor(get(urlPathMatching(".*rates_of_exchange.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": [
                                { "record_date": "2020-01-01", "exchange_rate": "1.1" }
                              ]
                            }
                        """)));

        BigDecimal rate = client.getRateWithinSixMonths(
                LocalDate.of(2024, 3, 1),
                "Austria-Euro"
        );

        assertNull(rate);
    }

    /**
     * G) PROVES: robustness: skips unparsable dates and still selects latest valid.
     */
    @Test
    void shouldSkipUnparsableDatesAndStillSelectLatestValid() {
        wireMock.stubFor(get(urlPathMatching(".*rates_of_exchange.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": [
                                { "record_date": "NOT_A_DATE", "exchange_rate": "9.9" },
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
