# Currency Conversion Service

A lightweight, production‑minded backend service for storing purchase transactions in USD and retrieving them converted into foreign currencies using authoritative exchange‑rate data from a public government API.

This project demonstrates clean API design, layered architecture, robust validation, and real‑world integration with an external data provider.

---

## Features

### Store Purchase Transactions
- Description (max 50 characters)
- Transaction date (validated)
- USD amount (positive, rounded to cents)
- Automatically assigned unique identifier
- Persisted in an embedded database

### Retrieve Converted Transactions
- Convert stored USD purchases into a target currency
- Uses authoritative exchange‑rate data
- Selects the latest rate **on or before** the purchase date
- Enforces a **6‑month look‑back window**
- Returns:
  - ID  
  - Description  
  - Transaction date  
  - Original USD amount  
  - Exchange rate used  
  - Converted amount (rounded to 2 decimals)

### Dynamic Currency Support
- No hard‑coded currency mappings
- Supported currencies derived from the external dataset
- Validates currency codes against dynamically loaded data

### Robust Validation & Error Handling
- Clear, consistent error responses
- Validation errors (400)
- Missing purchases (404)
- Missing exchange rates (400)
- External API failures (502)

---

## Architecture

The service follows a clean, maintainable layered architecture:

