
# Rate Limiter

A scalable, thread-safe distributed rate limiting system built using Spring Boot, Redis, and Lua scripting, designed for high-concurrency API protection in microservice architectures.

## Overview
This project implements a per-user API rate limiting middleware that ensures fair usage of system resources by restricting each user to 100 requests per minute. It is designed to work in distributed environments and handles concurrency safely using Redis atomic operations with Lua scripting.

## Tech Stack
- Java 17
- Spring Boot
- Redis
- Lua Scripting
- Spring Security (JWT-ready design)
- JUnit & Mockito
- Testcontainers

## Use Case
- Limits API usage to 100 requests per minute per user
- Works in a distributed system
- Handles high concurrency safely
- Returns proper HTTP status codes:
  - 200 OK → Allowed
  - 429 Too Many Requests → Rate limit exceeded
  - 401 Unauthorized → Missing user identity

## Rate Limiting Strategy

This project follows the Strategy Pattern using the RateLimiter interface, allowing multiple rate limiting algorithms to coexist under a common contract.

Currently supported strategies:
- Fixed Window Rate Limiter
- Token Bucket Rate Limiter

The active strategy is selected at runtime using Spring configuration (application.properties), enabling easy switching between algorithms without modifying the filter or controller layer.

## Detailed Flow

### Fixed Window Strategy:
- Request enters RateLimitingFilter
- Extract userId from request header
- Call RateLimiter.isAllowed(userId)
- Spring routes request to FixedWindowRateLimiter (based on strategy configuration)
- Redis Lua script executes atomically:
  - INCR counter
  - Set TTL (first request)
  - Validate limit
- Decision:
  -  Allow → request proceeds
  -  Block → HTTP 429 returned

### Token Bucket Strategy:
- Request enters RateLimitingFilter
- Extract userId from request header
- Call RateLimiter.isAllowed(userId)
- Spring routes request to TokenBucketRateLimiter (based on strategy configuration)
- Redis Lua script executes atomically:
  - Read current tokens and lastRefillTime from Redis Hash
  - Calculate time difference (now - last)
  - Refill tokens based on refill rate (100 tokens/min ≈ 1.67 tokens/sec)
  - If tokens ≥ 1:
    - Decrement token by 1
    - Update Redis state (tokens, last)
    - Return ALLOW
  - Else:
    - Return BLOCK
- Decision:
  -  Allow → request proceeds
  -  Block → HTTP 429 returned with Retry-After
    
## Layers of Application
```
Controller Layer

controller/
└── TestController

- Acts as the API entry point
- Used for validating rate limiting behavior
- Represents real-world business endpoints in production

Filter Layer (Core Interception Layer)


filters/
└── RateLimitingFilter.java

- Executes before controller layer
- Extracts user identity (userId)
- Calls rate limiting service
- Blocks or allows request execution
- Acts as a lightweight API Gateway

Service Layer (Strategy Pattern Implementation)

service/
├── RateLimiter (interface)
├── FixedWindowRateLimiter
├── TokenBucketRateLimiter
└── RateLimiterRouter

- Defines core rate limiting logic
- Uses Strategy Pattern
  - RateLimiter → interface layer
  - FixedWindowRateLimiter → active implementation
  - TokenBucketRateLimiter → extensible alternative
- Enables switching algorithms without modifying filter/controller
- RateLimiterRouter routes the request to the configured algorithm (FIXED_WINDOW / TOKEN_BUCKET)

Configuration Layer

config/
├── RedisScriptConfig
└── SecurityConfig


RedisScriptConfig:
- Defines Lua scripts as Spring Beans
- Ensures atomic execution in Redis

SecurityConfig:
- Disables CSRF for APIs
- Stateless architecture (JWT-ready design)

DTO Layer

dto/
└── ErrorResponse

- Standard response model for errors
- Used for:
  - HTTP 401 (Unauthorized)
  - HTTP 429 (Rate limit exceeded)
- Ensures consistent API response format

Utility Layer

exception/
└── ApiErrorResponseWriter

- Centralized error response handler
- Converts DTO → JSON response
- Eliminates duplication across filters/services
```
## Each user is tracked using

key = rate_limit:{userId}


Redis Lua Script Logic:
- INCR key
- IF first request → set EXPIRE 60s
- IF count > 100 → BLOCK
- ELSE → ALLOW

## Why Redis + Lua?

Without Lua scripting:
- INCR and EXPIRE are separate operations
- Race conditions occur under concurrent requests
- TTL inconsistencies possible

With Lua scripting:
- Entire operation is atomic
- Single network call to Redis
- Safe under high concurrency

## Testing Strategy

### Unit Tests (Mockito)
- Validate service logic
- Mock Redis responses (1/0/null)

### Filter Tests
- Validate HTTP 401 & 429 responses
- Ensure request blocking behavior

### Concurrency Tests (Testcontainers)
- Real Redis instance
- Multi-threaded request simulation
- Ensures atomic correctness under load

## Security Design
- User identity extracted via userId header
- Designed to be JWT-compatible
- Spring Security integrated (stateless, open for demo)

## Tradeoffs

### Fixed Window

Pros:
- Simple and efficient
- Easy to implement and reason about
- O(1) Redis operations
- Works well in distributed systems

Limitation (Boundary Problem):
At window boundaries:
- 100 requests at 10:59:59
- 100 requests at 11:00:01

This can cause burst traffic spikes.

Alternative Approaches:
- Sliding Window
  - More accurate
  - Prevents boundary bursts
  - Higher complexity
- Token Bucket
  - Industry standard
  - Smooth traffic control
  - Best for production-grade systems

## Design Decisions
- Filter-based interception for early request rejection
- Redis used as centralized distributed state store
- Lua scripting ensures atomic rate limiting logic
- Interface-based design allows strategy extension

## Assumptions
- userId is passed via request header (JWT-ready)
- Fixed window duration is 60 seconds
- Redis is always available and reliable
- Single rate limit rule per user (100/min)
- Retry-After is static (simplified for demo)

## How to Run

1. Start Redis
docker run -p 6379:6379 redis
2. Run Application
mvn spring-boot:run
3. Run Tests
mvn test
## API Example

### Request

GET /test
userId: user1
Responses

 Success

200 OK
Request successful

 Rate Limited
```
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again after 60 seconds"
}
```
 Missing User
```
{
  "status": 401,
  "error": "Unauthorized User",
  "message": "User Id is missing"
}
```

API to Test (cURL command)
```
curl http://localhost:8080/test -H "userId: user1"
```
## Test Report

- Unit Tests: Passed (Mockito)
- Filter Tests: Passed (401 / 429 / 200 validation)
- Concurrency Tests: Passed using Testcontainers (Redis)
- Coverage: Validates rate limiting under multi-threaded load

## Summary

This project demonstrates:

- Distributed system design using Redis
- Atomic operations using Lua scripting
- Thread-safe rate limiting under concurrency
- Clean Spring Boot filter-based architecture
- Extensible strategy-based design (Fixed / Sliding / Token Bucket ready)

## Diagramatic Representation

                         ┌─────────────────────┐
                        │      Client         │
                        │ (Browser / Postman) │
                        └─────────┬───────────┘
                                  │
                                  ▼
                ┌────────────────────────────────┐
                │ Spring Boot Filter Layer       │
                │ RateLimitingFilter             │
                └─────────────┬──────────────────┘
                              │
                ┌─────────────┴─────────────┐
                │ Extract userId from header │
                └─────────────┬─────────────┘
                              │
                 ┌────────────┴─────────────┐
                 │ userId missing?           │
                 └────────────┬─────────────┘
                              │
                 ┌────────────┴─────────────┐
                 │ YES                       │ NO
                 ▼                           ▼
         HTTP 401 Unauthorized     ┌───────────────────────────┐
        JSON Error Response          │ RateLimiterRouter         │
                                    │ (Strategy Selector)       │
                                    └─────────────┬─────────────┘
                                                  │
                           ┌──────────────────────┴──────────────────────┐
                           │                                             │
                           ▼                                             ▼
            ┌───────────────────────────────┐           ┌───────────────────────────────┐
            │ FixedWindowRateLimiter        │           │ TokenBucketRateLimiter        │
            │ (INCR + EXPIRE)               │           │ (tokens + refill logic)       │
            └─────────────┬─────────────────┘           └─────────────┬─────────────────┘
                          │                                           │
                          ▼                                           ▼
              ┌────────────────────────┐                 ┌────────────────────────┐
              │ Redis Lua Script        │                 │ Redis Lua Script        │
              │ INCR + EXPIRE + LIMIT   │                 │ HMGET + refill + HSET   │
              └──────────┬─────────────┘                 └──────────┬─────────────┘
                         │                                           │
             ┌───────────┴────────────┐                  ┌───────────┴────────────┐
             │                        │                  │                        │
             ▼                        ▼                  ▼                        ▼
      ALLOW REQUEST          BLOCK REQUEST        ALLOW REQUEST         BLOCK REQUEST
        (200 OK)               (HTTP 429)            (200 OK)               (HTTP 429)
