package com.example.wex.integration;

import com.example.wex.model.PurchaseCreateRequest;
import com.example.wex.model.PurchaseResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

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

        String descriptor = (String) currencies.getFirst();

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
}