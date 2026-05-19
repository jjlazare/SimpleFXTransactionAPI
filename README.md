# 🧾 WEX Purchase Transaction Service

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Gradle](https://img.shields.io/badge/Build-Gradle-success)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## 📑 Table of Contents

- [Setup and Run (Docker - Recommended)](#setup-and-run-docker---recommended)
- [Overview](#overview)
- [Architecture](#architecture)
- [API Usage](#api-usage)
- [API Documentation (Swagger / OpenAPI)](#api-documentation-swagger--openapi)
- [Testing Strategy](#testing-strategy)
- [Error Handling](#error-handling)
- [Design Decisions](#design-decisions)
- [Future Improvements](#future-improvements)
- [Summary](#summary)

---

## 🚀 Setup and Run (Docker - Recommended)

```bash
docker compose up --build
```

### ✅ What this does
- Builds the application container  
- Starts PostgreSQL  
- Launches the API  
- Sets up networking automatically  

---

### 🌐 Application Access

The API runs internally on port **8484**, and is exposed via Docker on:

👉 **https://localhost:8080**

---

### 🛑 Stop

```bash
docker compose down
```

---

### 📝 Notes
- No local database installation required  
- PostgreSQL runs entirely in Docker  
- Data persists via Docker volumes  

---

## 📘 Overview

Backend API for managing purchase transactions and performing currency conversion using U.S. Treasury exchange rates.

### 🔑 Capabilities
- Persist transactions in USD  
- Retrieve supported currency descriptors  
- Convert using historical rates  
- Strict validation and error handling  

---

## 🏗️ Architecture

### 🧱 Layers

| Layer            | Responsibility |
|-----------------|--------------|
| Controller      | HTTP handling |
| Service         | Business logic |
| Repository      | Persistence |
| Treasury Client | External API |
| Cache           | Currency descriptor caching |

---

### 📦 Deployment

```
[ Docker ]
   ├── App (Spring Boot - Port 8080)
   └── DB (PostgreSQL)
```

- Docker networking connects services  
- Database hostname: `postgres`  
- External access via port **8080 → 8484 mapping**  

---

## 🔌 API Usage

### ➕ Create Purchase
**POST** `/api/v1/purchases`

```json
{
  "description": "Office Chair",
  "transactionDate": "2026-03-15",
  "amountUsd": "150.75"
}
```

---

### 🌍 Currency Descriptions
**GET** `/api/v1/treasury/currency-descriptions`

---

### 💱 Convert Purchase  
**GET**  
`/api/v1/purchases/{id}?country_currency_desc=Austria-Euro`

---

## 📄 API Documentation (Swagger / OpenAPI)

- Swagger UI: https://localhost:8080/swagger-ui/index.html  
- OpenAPI Spec: https://localhost:8080/v3/api-docs  

---

## 🧪 Testing

```bash
./gradlew test
```

- Unit tests  
- Integration tests  
- WireMock for Treasury API  

---

## ⚠️ Error Handling

| Code | Description |
|------|------------|
| 400  | Validation error |
| 404  | Not found |
| 502  | External API failure |
| 503  | Treasury unavailable |

---

## 🧠 Design Decisions

### 💾 Database
- PostgreSQL across environments  

### 🐳 Docker First
- Fully containerized  

### 💽 Persistence
- Docker volumes  

### 🔄 Schema
- Hibernate auto-update  

---

### 🔁 Idempotency and Duplicate Transaction Handling

Each `POST /purchases` request is treated as a request to create a **new transaction**.

The requirements do not define deduplication rules, so:

- Every request generates a unique identifier  
- All transactions are stored independently  

#### Why deduplication is not inferred

- Identical transactions may be valid  
- Timestamps are not reliable indicators  
- Payload data does not convey request intent  

#### Production-ready approach (not implemented)

- Idempotency keys (client-provided)  
- Client-generated transaction IDs with uniqueness constraints  

These require API contract changes and were out of scope.

---

### ⚡ Caching of Currency Descriptions

The service maintains an **in-memory cache** of Treasury currency descriptors.

#### Benefits

**Performance**
- Eliminates repeated API calls  
- Enables fast validation  

**Availability**
- Reduces dependency on Treasury uptime  
- Continues operating with cached data  

**Resilience**
- Cache refresh can fail without breaking the service  

**Testability**
- Tests can dynamically fetch valid descriptors  

#### Scope

- Only descriptors are cached  
- Exchange rates are fetched at runtime for accuracy  

---

### 🔒 Data Integrity
- Strict currency descriptor matching  

### 📉 Exchange Rules
- 6‑month rate window  

---

## 🚧 Future Improvements

- Flyway / Liquibase migrations  
- Authentication & authorization  
- Metrics and logging  
- Pagination & filtering  

---

## ✅ Summary

- Containerized backend  
- PostgreSQL persistence  
- External API integration  
- Clean layered design  
- Reproducible environment  

---

## ▶️ Run

```bash
docker compose up --build
```
