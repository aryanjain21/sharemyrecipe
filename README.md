# ShareMyRecipe Platform

A modern recipe publishing platform built with **Spring Boot 3**, **PostgreSQL**, and **RabbitMQ**.  
It exposes a secure REST API with JWT authentication, image upload with automatic thumbnail generation, chef following, and async recipe publishing via a background worker.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Quick Start (Docker Compose)](#quick-start-docker-compose)
6. [Local Development Setup](#local-development-setup)
   - [1. Start Infrastructure](#1-start-infrastructure)
   - [2. Run the API](#2-run-the-api)
   - [3. Run the Worker](#3-run-the-worker)
7. [Configuration Reference](#configuration-reference)
8. [API Documentation](#api-documentation)
9. [API Endpoints](#api-endpoints)
   - [Auth](#auth)
   - [Recipes (Public)](#recipes-public)
   - [Recipe Feed (Followers)](#recipe-feed-followers)
   - [Chef Authoring (JWT Protected)](#chef-authoring-jwt-protected)
   - [Image Upload (JWT Protected)](#image-upload-jwt-protected)
   - [Chef Profiles & Following](#chef-profiles--following)
10. [Authentication Flow](#authentication-flow)
11. [Async Publishing Flow](#async-publishing-flow)
12. [Image Upload & Resizing](#image-upload--resizing)
13. [Roles & Permissions](#roles--permissions)
14. [Running Tests](#running-tests)
15. [Environment Variables](#environment-variables)

---

## Architecture Overview

```
┌─────────────┐     REST/HTTP      ┌────────────────────┐
│   Client    │ ──────────────────▶│  sharemyrecipe-api │
└─────────────┘                    │  (Spring Boot 3)   │
                                   │  port 8080         │
                                   └────────┬───────────┘
                                            │  publish event
                                            ▼
                                   ┌────────────────────┐
                                   │     RabbitMQ       │
                                   │  recipe.exchange   │
                                   │  recipe.publish.   │
                                   │  queue             │
                                   └────────┬───────────┘
                                            │  consume
                                            ▼
                                   ┌────────────────────┐
                                   │sharemyrecipe-worker│
                                   │  (Spring Boot 3)   │
                                   │  port 8081         │
                                   └────────┬───────────┘
                                            │  update status
                                            ▼
                                   ┌────────────────────┐
                                   │    PostgreSQL       │
                                   │  (shared DB)       │
                                   └────────────────────┘
```

Both applications share the **same PostgreSQL database**.  
The API creates recipes in `DRAFT` state and publishes a `RecipePublishEvent` to RabbitMQ.  
The worker consumes the event and transitions the recipe to `PUBLISHED`.

---

## Tech Stack

| Component        | Technology                         |
| ---------------- | ---------------------------------- |
| API Framework    | Spring Boot 3.2 / Spring MVC       |
| Security         | Spring Security + JJWT (JWT HS256) |
| Database ORM     | Spring Data JPA + Hibernate 6      |
| Database         | PostgreSQL 16                      |
| Migrations       | Flyway                             |
| Messaging        | RabbitMQ 3.13 + Spring AMQP        |
| Image Processing | Thumbnailator                      |
| API Docs         | SpringDoc OpenAPI 3 (Swagger UI)   |
| Build            | Maven 3                            |
| Runtime          | Java 21 (Eclipse Temurin)          |
| Containerization | Docker + Docker Compose            |

---

## Project Structure

```
shareMyRecipe/
├── docker-compose.yml                    # Full stack orchestration
├── README.md
│
├── sharemyrecipe-api/                    # Main REST API application
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/sharemyrecipe/
│       ├── ShareMyRecipeApiApplication.java
│       ├── config/
│       │   ├── OpenApiConfig.java        # Swagger/OpenAPI setup
│       │   └── WebMvcConfig.java         # Static file serving for uploads
│       ├── controller/
│       │   ├── AuthController.java       # /api/v1/auth/**
│       │   ├── RecipeController.java     # /api/v1/recipes/** + /api/v1/chef/recipes/**
│       │   ├── ImageController.java      # Image upload endpoints
│       │   └── ChefController.java       # Chef profiles + follow/unfollow
│       ├── domain/                       # JPA entities
│       │   ├── User.java
│       │   ├── Recipe.java
│       │   ├── RecipeImage.java
│       │   ├── Follow.java / FollowId.java
│       │   ├── RefreshToken.java
│       │   ├── Role.java
│       │   └── RecipeStatus.java
│       ├── dto/                          # Request/Response records
│       ├── exception/                    # Custom exceptions + GlobalExceptionHandler
│       ├── messaging/                    # RabbitMQ config + event publisher
│       ├── repository/                   # Spring Data JPA repositories
│       ├── security/                     # JWT filter, SecurityConfig, UserDetailsService
│       └── service/                      # Business logic
│
├── sharemyrecipe-worker/                 # Async worker application
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/sharemyrecipe/worker/
│       ├── ShareMyRecipeWorkerApplication.java
│       ├── domain/Recipe.java            # Minimal entity projection
│       ├── messaging/
│       │   ├── RabbitMQConfig.java
│       │   ├── RecipePublishEvent.java
│       │   └── RecipePublishConsumer.java # @RabbitListener
│       ├── repository/RecipeRepository.java
│       └── service/RecipeIngestionService.java
```

---

## Prerequisites

- **Java 21** (or later) — [Download](https://adoptium.net/)
- **Maven 3.9+** — [Download](https://maven.apache.org/)
- **Docker Desktop** (for Docker Compose path) — [Download](https://www.docker.com/products/docker-desktop/)
- **Optional (local dev)**: PostgreSQL 16, RabbitMQ 3.13

---

## Quick Start (Docker Compose)

This is the **fastest** way to run the complete stack.

```bash
# Clone / navigate to the project root
cd shareMyRecipe

# Build and start all services
docker compose up --build

# Or run in background
docker compose up --build -d
```

Services started:

| Service    | URL                                              |
| ---------- | ------------------------------------------------ |
| API        | http://localhost:8080                            |
| Worker     | http://localhost:8081                            |
| Swagger UI | http://localhost:8080/swagger-ui.html            |
| RabbitMQ   | http://localhost:15672 (smr_user/smr_pass)       |
| PostgreSQL | localhost:5432 (smr_user/smr_pass/sharemyrecipe) |

To stop:

```bash
docker compose down
# To also remove volumes (wipes database & uploads)
docker compose down -v
```

---

## Local Development Setup

### 1. Start Infrastructure

Start only PostgreSQL and RabbitMQ via Docker:

```bash
docker compose up postgres rabbitmq -d
```

### 2. Run the API

```bash
cd sharemyrecipe-api

# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/sharemyrecipe-api-1.0.0.jar
```

The API starts on **http://localhost:8080**.

**Override settings via environment variables (optional):**

```bash
APP_JWT_SECRET=my-super-secret-key-32chars-minimum \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sharemyrecipe \
mvn spring-boot:run
```

### 3. Run the Worker

```bash
cd sharemyrecipe-worker

# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/sharemyrecipe-worker-1.0.0.jar
```

The Worker starts on **http://localhost:8081**.

---

## Configuration Reference

### API (`sharemyrecipe-api/src/main/resources/application.yml`)

| Property                           | Default                | Description                            |
| ---------------------------------- | ---------------------- | -------------------------------------- |
| `app.jwt.secret`                   | (required – 32+ chars) | HMAC-SHA256 signing key for JWT        |
| `app.jwt.access-ttl-ms`            | `900000` (15 min)      | Access token TTL in milliseconds       |
| `app.jwt.refresh-ttl-ms`           | `604800000` (7 days)   | Refresh token TTL in milliseconds      |
| `app.upload.dir`                   | `./uploads`            | Directory for uploaded images          |
| `app.email-verification-enabled`   | `false`                | Toggle email verification (v2 feature) |
| `app.pagination.default-page-size` | `20`                   | Default page size for list endpoints   |
| `app.pagination.max-page-size`     | `100`                  | Maximum allowed page size              |

### Worker (`sharemyrecipe-worker/src/main/resources/application.yml`)

| Property                                                 | Default  | Description                      |
| -------------------------------------------------------- | -------- | -------------------------------- |
| `spring.rabbitmq.listener.simple.retry.max-attempts`     | `5`      | Max delivery attempts before DLQ |
| `spring.rabbitmq.listener.simple.retry.initial-interval` | `2000ms` | Initial retry delay              |
| `spring.rabbitmq.listener.simple.retry.multiplier`       | `2.0`    | Exponential backoff multiplier   |

---

## API Documentation

Interactive Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI JSON:

```
http://localhost:8080/v3/api-docs
```

---

## API Endpoints

### Auth

| Method | Endpoint               | Auth   | Description                         |
| ------ | ---------------------- | ------ | ----------------------------------- |
| POST   | `/api/v1/auth/signup`  | Public | Register (role: `USER` or `CHEF`)   |
| POST   | `/api/v1/auth/login`   | Public | Get access + refresh tokens         |
| POST   | `/api/v1/auth/refresh` | Public | Exchange refresh token for new pair |
| POST   | `/api/v1/auth/logout`  | JWT    | Revoke all refresh tokens           |

**Sign-up example:**

```json
POST /api/v1/auth/signup
{
  "email": "alice@example.com",
  "handle": "alice_chef",
  "displayName": "Alice Baker",
  "password": "Str0ngP@ss!",
  "role": "CHEF"
}
```

### Recipes (Public)

| Method | Endpoint               | Auth   | Description                                      |
| ------ | ---------------------- | ------ | ------------------------------------------------ |
| GET    | `/api/v1/recipes`      | Public | List published recipes with filters + pagination |
| GET    | `/api/v1/recipes/{id}` | Public | Get a single published recipe                    |

**Query parameters for `GET /api/v1/recipes`:**

| Parameter       | Type     | Description                                            |
| --------------- | -------- | ------------------------------------------------------ |
| `q`             | string   | Keyword search (title, summary, ingredients, steps)    |
| `chefId`        | UUID     | Filter by chef ID                                      |
| `chefHandle`    | string   | Filter by chef handle (e.g. `alice_chef`)              |
| `publishedFrom` | ISO date | Filter by published date (e.g. `2025-01-01T00:00:00Z`) |
| `publishedTo`   | ISO date | Filter by published date                               |
| `page`          | int      | Page index (0-based, default: 0)                       |
| `pageSize`      | int      | Items per page (default: 20, max: 100)                 |

**Pagination meta in every response:**

```json
{
  "content": [...],
  "pagination": {
    "page": 0,
    "pageSize": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### Recipe Feed (Followers)

| Method | Endpoint       | Auth | Description                               |
| ------ | -------------- | ---- | ----------------------------------------- |
| GET    | `/api/v1/feed` | JWT  | Published recipes from all followed chefs |

Same query params as public recipes (`q`, `publishedFrom`, `publishedTo`, `page`, `pageSize`).

### Chef Authoring (JWT Protected)

> Requires `CHEF` or `ADMIN` role.

| Method | Endpoint                            | Description                                              |
| ------ | ----------------------------------- | -------------------------------------------------------- |
| GET    | `/api/v1/chef/recipes`              | List own recipes (all states)                            |
| POST   | `/api/v1/chef/recipes`              | Create recipe (set `publish: true` to queue immediately) |
| PATCH  | `/api/v1/chef/recipes/{id}`         | Update a recipe                                          |
| POST   | `/api/v1/chef/recipes/{id}/publish` | Queue a draft recipe for publishing                      |
| DELETE | `/api/v1/chef/recipes/{id}`         | Delete a recipe                                          |

**Create recipe example:**

```json
POST /api/v1/chef/recipes
Authorization: Bearer <access_token>

{
  "title": "Fluffy Pancakes",
  "summary": "Quick weekend breakfast",
  "ingredients": "2 cups flour, 2 eggs, 1 cup milk, 1 tbsp sugar, pinch of salt",
  "steps": "1. Mix dry ingredients. 2. Add wet ingredients. 3. Cook on medium heat.",
  "labels": ["breakfast", "vegetarian", "quick"],
  "publish": false
}
```

### Image Upload (JWT Protected)

| Method | Endpoint                                      | Description                              |
| ------ | --------------------------------------------- | ---------------------------------------- |
| POST   | `/api/v1/chef/recipes/{recipeId}/images`      | Upload 1..n images (multipart/form-data) |
| DELETE | `/api/v1/chef/recipes/{recipeId}/images/{id}` | Delete an image                          |

```bash
curl -X POST http://localhost:8080/api/v1/chef/recipes/{recipeId}/images \
  -H "Authorization: Bearer <token>" \
  -F "files=@pancakes.jpg" \
  -F "files=@batter.png"
```

Images are stored in the upload directory. A thumbnail (400×300px, aspect-ratio preserved) is generated automatically.

### Chef Profiles & Following

| Method | Endpoint                          | Auth   | Description                |
| ------ | --------------------------------- | ------ | -------------------------- |
| GET    | `/api/v1/chefs/{id}`              | Public | Get chef profile by ID     |
| GET    | `/api/v1/chefs/handle/{handle}`   | Public | Get chef profile by handle |
| GET    | `/api/v1/chefs/{id}/stats`        | Public | Follower/following counts  |
| GET    | `/api/v1/chefs/{id}/followers`    | Public | List followers             |
| GET    | `/api/v1/chefs/{id}/following`    | Public | List following             |
| POST   | `/api/v1/chefs/{targetId}/follow` | JWT    | Follow a chef              |
| DELETE | `/api/v1/chefs/{targetId}/follow` | JWT    | Unfollow a chef            |

---

## Authentication Flow

```
1. POST /api/v1/auth/signup    → creates account
2. POST /api/v1/auth/login     → returns { accessToken, refreshToken, ... }
3. Use Authorization: Bearer <accessToken> on protected endpoints
4. When access token expires (15 min), call POST /api/v1/auth/refresh with the refreshToken
5. POST /api/v1/auth/logout    → revokes all refresh tokens
```

**Token lifetimes:**

- Access token: **15 minutes**
- Refresh token: **7 days** (rotated on each refresh)

---

## Async Publishing Flow

```
Chef                  API                 RabbitMQ              Worker              DB
 │                     │                      │                      │               │
 │─POST /publish──────▶│                      │                      │               │
 │                     │─publish event───────▶│                      │               │
 │◀─202 Accepted───────│                      │─deliver event───────▶│               │
 │                     │                      │                      │─UPDATE status▶│
 │                     │                      │                      │  → PUBLISHED  │
```

1. Chef calls `POST /api/v1/chef/recipes/{id}/publish`
2. API persists recipe as `DRAFT` and emits `RecipePublishEvent` to `recipe.exchange`
3. Worker consumes the event from `recipe.publish.queue`
4. Worker sets status → `PUBLISHED` and `published_at` = now
5. Recipe becomes visible in public feeds

**Dead Letter Queue:** If the worker fails after 5 retries (with exponential back-off), the message is routed to `recipe.publish.dlq` for investigation.

---

## Image Upload & Resizing

- Accepted formats: `image/jpeg`, `image/png`, `image/webp`
- Max file size: **10 MB** per file, **40 MB** per request
- On upload, [Thumbnailator](https://github.com/coobird/thumbnailator) generates a **400×300 px** thumbnail (aspect-ratio preserved)
- Files are stored in `${app.upload.dir}` (default: `./uploads`)
- URLs are served at `/uploads/<filename>`

---

## Roles & Permissions

| Role    | Can Browse Recipes | Can Author Recipes | Can Follow | Can Moderate |
| ------- | ------------------ | ------------------ | ---------- | ------------ |
| `USER`  | ✅                 | ❌                 | ✅         | ❌           |
| `CHEF`  | ✅                 | ✅ (own only)      | ✅         | ❌           |
| `ADMIN` | ✅                 | ✅ (all)           | ✅         | ✅           |

- The `role` field is set at sign-up. Valid values: `USER`, `CHEF` (self-promotion to `ADMIN` is blocked).
- Admins can edit or delete any recipe.
- Chefs can only edit/delete their own recipes.

---

## Running Tests

```bash
# API tests
cd sharemyrecipe-api
mvn test

# Worker tests
cd sharemyrecipe-worker
mvn test
```

---

## Environment Variables

Override any `application.yml` property via environment variable (Spring Boot convention: replace `.` and `-` with `_`, uppercase).

### API

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sharemyrecipe
SPRING_DATASOURCE_USERNAME=smr_user
SPRING_DATASOURCE_PASSWORD=smr_pass
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=smr_user
SPRING_RABBITMQ_PASSWORD=smr_pass
APP_JWT_SECRET=change-me-in-production-must-be-at-least-32-chars
APP_JWT_ACCESS_TTL_MS=900000
APP_JWT_REFRESH_TTL_MS=604800000
APP_UPLOAD_DIR=./uploads
APP_EMAIL_VERIFICATION_ENABLED=false
```

### Worker

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sharemyrecipe
SPRING_DATASOURCE_USERNAME=smr_user
SPRING_DATASOURCE_PASSWORD=smr_pass
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=smr_user
SPRING_RABBITMQ_PASSWORD=smr_pass
```

> **Production note:** Always override `APP_JWT_SECRET` with a cryptographically random 32+ character secret. Never commit secrets to version control.
