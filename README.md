# Distributed URL Shortener & Analytics Platform

> A production-grade distributed URL shortener built with Java 21, Spring Boot 3, Redis, Apache Kafka, PostgreSQL, Docker, and Prometheus — designed to handle high-throughput redirect workloads with real-time click analytics and full observability.

![CI Pipeline](https://github.com/sarveshh31/url-shortener/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![Redis](https://img.shields.io/badge/Redis-7-red)
![Kafka](https://img.shields.io/badge/Kafka-3.7-black)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## Benchmark Results

| Metric | Without Redis Cache | With Redis Cache | Improvement |
|--------|-------------------|-----------------|-------------|
| Avg latency | 360ms | 162ms | **55% faster** |
| Throughput | 998 req/sec | 1,160 req/sec | **+16%** |
| Std deviation | 147ms | 99ms | **32% more consistent** |
| Error rate | 0% | 0% | — |
| Total samples | 100,000 | 100,000 | 500 concurrent threads |

> Benchmarks run using Apache JMeter 5.6 · 500 threads · 30s ramp-up · 200 loops · screenshots in `/benchmarks`

---

## Architecture

```
Client / Browser
      │
      ▼
┌─────────────────────────────────┐
│   Spring Boot 3 REST API        │
│   JWT Auth · Rate Limiting      │
└───────┬────────────┬────────────┘
        │            │
        ▼            ▼
   ┌─────────┐  ┌──────────────┐
   │  Redis  │  │  Kafka Topic │
   │  Cache  │  │ click-events │
   │  7.x    │  │ 3 partitions │
   └────┬────┘  └──────┬───────┘
        │              │
        ▼              ▼
   ┌──────────────────────────┐
   │      PostgreSQL 15       │
   │  urls · users ·          │
   │  click_events tables     │
   └──────────────────────────┘
        │
        ▼
   ┌────────────┐
   │ Prometheus │ ← scrapes /actuator/prometheus every 15s
   └────────────┘
```

**Request flow — redirect (hot path):**
1. `GET /{shortCode}` arrives
2. JWT filter validates token (public endpoint — skipped)
3. Rate limiter checks Redis sorted set — 100 req/min per IP
4. Cache lookup in Redis (`url:{shortCode}`) → hit returns in ~1ms
5. On cache miss → PostgreSQL query → warm cache → return URL
6. Kafka producer fires `ClickEventMessage` (non-blocking)
7. `HTTP 302` redirect returned to client
8. Kafka consumer writes click data to `click_events` table asynchronously

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Language | Java 21 | Core service |
| Framework | Spring Boot 3.3 | REST API, DI, Security |
| Cache | Redis 7 | URL cache, rate limiting (sorted sets) |
| Message Queue | Apache Kafka 3.7 | Async click event pipeline |
| Database | PostgreSQL 15 | Persistent storage |
| Auth | Spring Security + JWT (JJWT 0.12) | Stateless authentication |
| Observability | Prometheus + Spring Actuator | Metrics scraping |
| Containerisation | Docker + Docker Compose | Full stack deployment |
| CI/CD | GitHub Actions | Build, test, Docker image |
| Load Testing | Apache JMeter 5.6 | Benchmarks |

---

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/auth/register` | None | Register new user |
| `POST` | `/api/v1/auth/login` | None | Login, returns JWT token |
| `POST` | `/api/v1/shorten` | JWT | Create short URL |
| `GET` | `/{shortCode}` | None | Redirect to original URL |
| `GET` | `/api/v1/urls/my` | JWT | List your shortened URLs |
| `GET` | `/api/v1/analytics/{shortCode}` | JWT | Click stats for a URL |
| `GET` | `/api/v1/analytics/top` | JWT | Top 10 URLs by clicks |
| `GET` | `/actuator/health` | None | Health check |
| `GET` | `/actuator/prometheus` | None | Prometheus metrics |

---

## Running Locally — One Command

### Prerequisites
- Docker Desktop installed and running
- Nothing else — Docker handles Java, PostgreSQL, Redis, Kafka, Prometheus

### Start the full stack

```bash
git clone https://github.com/sarveshh31/url-shortener.git
cd url-shortener
docker-compose up --build
```

First run downloads all images and builds the app (~3-5 min). Subsequent runs take ~30 seconds.

### Verify everything is up

```bash
# all 6 containers should show STATUS = Up
docker ps
```

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| Health | http://localhost:8080/actuator/health |
| Metrics | http://localhost:8080/actuator/prometheus |
| Prometheus | http://localhost:9090 |

### Stop

```bash
docker-compose down
```

---

## Quickstart — Test with Postman

**1. Register**
```http
POST localhost:8080/api/v1/auth/register
Content-Type: application/json

{
  "email": "test@test.com",
  "password": "123456",
  "name": "Sarvesh"
}
```

**2. Login — copy the token**
```http
POST localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "test@test.com",
  "password": "123456"
}
```

**3. Shorten a URL**
```http
POST localhost:8080/api/v1/shorten
Authorization: Bearer <your_token>
Content-Type: application/json

{
  "originalUrl": "https://github.com/sarveshh31",
  "expiryDays": 30
}
```

**4. Use the short URL** — paste `localhost:8080/{shortCode}` in browser → redirects instantly

**5. Check analytics**
```http
GET localhost:8080/api/v1/analytics/{shortCode}
Authorization: Bearer <your_token>
```

---

## Project Structure

```
src/main/java/com/sarvesh/urlshortener/
├── config/
│   ├── KafkaConfig.java          # Kafka consumer factory
│   ├── RedisConfig.java          # Redis template setup
│   └── SecurityConfig.java       # JWT + Spring Security rules
├── controller/
│   ├── AuthController.java       # /api/v1/auth/**
│   └── UrlController.java        # shorten, redirect, analytics
├── dto/
│   ├── AuthRequest.java
│   ├── AuthResponse.java
│   ├── ClickEventMessage.java    # Kafka message object
│   ├── ShortenRequest.java
│   └── ShortenResponse.java
├── entity/
│   ├── ClickEvent.java           # click_events table
│   ├── Url.java                  # urls table
│   └── User.java                 # users table
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/
│   ├── ClickEventRepository.java
│   ├── UrlRepository.java
│   └── UserRepository.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthFilter.java
│   └── JwtUtil.java
├── service/
│   ├── AsyncService.java         # async click count updates
│   ├── AuthService.java
│   ├── CacheService.java         # Redis cache + rate limiter
│   ├── KafkaConsumerService.java
│   ├── KafkaProducerService.java
│   └── UrlService.java
└── util/
    └── Base62Encoder.java        # short code generation
```

---

## Key Design Decisions

**Why Base62 for short codes?**
62^6 = 56.8 billion possible codes. Human-readable, URL-safe, compact. Collision detection via DB uniqueness constraint with retry loop.

**Why Redis cache-aside over write-through?**
Write-through updates cache on every write — simple but wastes memory on URLs that are never clicked again. Cache-aside only caches on first read, letting cold URLs expire naturally via TTL.

**Why Kafka over direct DB write for analytics?**
Synchronous DB write on every redirect creates a write bottleneck — every redirect waits for the analytics write before returning. Kafka decouples them: redirect returns in <5ms, analytics writes happen asynchronously. Under load this eliminates the DB as a bottleneck on the hot path.

**Why sliding window rate limiting over fixed window?**
Fixed window allows bursting at window boundaries (100 requests in last second of window + 100 in first second of next = 200 in 2 seconds). Sliding window tracks a rolling 60-second window using Redis sorted sets, preventing this attack.

---

## Benchmarks

JMeter test screenshots are in the `/benchmarks` directory:
- `Test-A.png` — baseline (no Redis cache, direct PostgreSQL)
- `Test-B.png` — with Redis cache enabled

**Test configuration:** 500 threads · 30s ramp-up · 200 loops · `GET /{shortCode}` endpoint · both redirects disabled (measuring 302 response time only)

---

## CI/CD Pipeline

GitHub Actions runs on every push to `main`:

1. Checkout code
2. Setup Java 21 (Temurin)
3. Maven build (`mvn clean package -DskipTests`)
4. Run tests with PostgreSQL service container
5. Build Docker image

---

## Environment Variables

When running without Docker Compose, set these:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/url_shortener_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_DATA_REDIS_HOST=localhost
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
APP_JWT_SECRET=your_secret_key_min_32_chars
APP_JWT_EXPIRATION=86400000
```

---

## Author

**Sarvesh Tiwari**
B.Tech Information Technology · RCOEM Nagpur · 2026
[LeetCode 1740](https://leetcode.com/u/sarvesh_3108/) · AWS Cloud Practitioner · OCI GenAI Professional
