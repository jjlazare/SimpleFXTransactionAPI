WEX Purchase Transaction Service Documentation

- [Overview](#overview)
- [Architecture](#architecture)
- [Setup and Run](#setup-and-run)
- [API Usage](#api-usage)
  - [Create Purchase](#create-purchase)
  - [Get Currency Descriptions](#get-currency-descriptions)
  - [Convert Purchase](#convert-purchase)
- [Testing](#testing)
- [Error Handling](#error-handling)
- [Design Decisions](#design-decisions)
- [Future Improvements](#future-improvements)
============================================
OVERVIEW
============================================

This service allows you to:
- Create purchase transactions in USD
- Convert purchases using U.S. Treasury exchange rates
- Validate inputs and handle errors consistently

Core flow:
1. Create a purchase
2. Retrieve a supported currency descriptor
3. Convert using Treasury exchange rates (within 6-month window)


============================================
TABLE OF CONTENTS
============================================
1. Overview
2. Architecture
3. Setup and Run
4. API Usage
5. Testing
6. Error Handling
7. Design Decisions


============================================
ARCHITECTURE
============================================

Layers:
- Controller: Handles HTTP requests
- Service: Business logic and orchestration
- Repository: JPA persistence (H2 DB)
- Treasury Client: External API integration
- Cache: Stores currency descriptors in memory

Notes:
- Currency descriptors must exactly match Treasury values
- No internal mapping is applied


============================================
SETUP AND RUN
============================================

Prerequisites:
- Java 21
- Gradle

Run locally:
    ./gradlew bootRun

Application URL:
    https://localhost:8443

H2 Console:
    http://localhost:8443/h2-console


============================================
API USAGE
============================================

1. Create Purchase
POST /api/v1/purchases

Example body:
{
  "description": "Office Chair",
  "transactionDate": "2026-03-15",
  "amountUsd": "150.75"
}

2. Get Currency Descriptions
GET /api/v1/treasury/currency-descriptions

3. Convert Purchase
GET /api/v1/purchases/{id}?country_currency_desc=Austria-Euro


============================================
TESTING
============================================

Test Types:

1. Unit Tests
- Validate business logic
- Validate error handling

2. Integration Tests
- End-to-end flow testing
- Uses in-memory DB
- Uses WireMock to simulate Treasury API

Test Scenarios Covered:
- Successful conversion
- Unsupported currency
- Missing purchase (404)
- No rate available
- Decimal rounding behavior

Run tests:
    ./gradlew test

Manual Testing Examples:

POST https://localhost:8443/api/v1/purchases
GET  https://localhost:8443/api/v1/treasury/currency-descriptions
GET  https://localhost:8443/api/v1/purchases/{id}?country_currency_desc=Brazil-Reals


============================================
ERROR HANDLING
============================================

Status Codes:
- 400: Validation error
- 404: Purchase not found
- 503: Treasury data unavailable
- 502: Treasury API failure


============================================
DESIGN DECISIONS
============================================

Cache Usage:
- Reduces external API calls
- Improves performance

Strict Descriptor Validation:
- Prevents mismatches
- Ensures consistency with Treasury API

6-Month Exchange Rule:
- Uses latest rate within 6 months of purchase
- Ensures realistic conversion accuracy


============================================
FUTURE IMPROVEMENTS
============================================

- Add retry/backoff for Treasury API
- Persist conversion history
- Replace H2 with production database
- Add authentication and security