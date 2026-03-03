# 🗄️ Redis Storage Monitor

> A Spring Boot service that monitors Redis memory utilization in real-time — with full support for both **Standalone** and **Cluster** deployments.

**Author:** Akhanda Pal Biswas

---

## 📌 Overview

The **Redis Storage Monitor** is a production-ready Spring Boot microservice that:

- Connects to Redis and reads memory metrics via `INFO memory`
- Calculates memory utilization as a percentage
- Supports **Standalone Redis** and **Redis Cluster** (with prefixed node metrics)
- Enforces a configurable threshold (default: **10%**)
- Returns meaningful HTTP status codes per scenario

---

## 🚦 API Response Matrix

| Scenario | HTTP Code | Status |
|---|---|---|
| Redis healthy & below threshold | `200 OK` | `HEALTHY` |
| Memory utilization ≥ 10% | `429 Too Many Requests` | `THRESHOLD_EXCEEDED` |
| Redis unreachable | `503 Service Unavailable` | `REDIS_UNAVAILABLE` |

---

## 🧠 Why This Project?

In production environments, Redis can be deployed in two ways:

- **Standalone** — single node, flat key metrics
- **Cluster** — multiple nodes, metrics prefixed per node (e.g., `10.0.0.1:6379.used_memory`)

The standard flat-key parsing logic **fails silently** in cluster mode. This service solves that by:

- Dynamically detecting the deployment type
- Parsing prefixed cluster keys and grouping metrics per node
- Computing utilization **per node**, then returning the **maximum**
- Avoiding hotspot masking (averaging hides saturated shards)

> **Why maximum and not average?**  
> The most saturated shard becomes the bottleneck. Averaging hides hotspots and creates a false sense of safety.

---

## 🏗️ Architecture

```
StorageController  →  RedisStorageMonitorService  →  RedisDatabaseService  →  Redis
```

| Layer | Responsibility |
|---|---|
| `StorageController` | Handles HTTP requests and maps status codes |
| `RedisStorageMonitorService` | Business logic — threshold checks & cluster parsing |
| `RedisDatabaseService` | Retrieves `INFO memory` from Redis |

---

## 📦 Project Structure

```
redis-storage-monitor/
├── controller/
├── service/
├── test/
├── pom.xml
└── README.md
```

---

## ⚙️ Prerequisites

| Requirement | Version |
|---|---|
| Java | 21 (LTS) |
| Maven | Latest |
| Docker Desktop | Running |

> ⚠️ Make sure your proxy is **disabled** if it blocks `localhost`.

---

## 🐳 Step 1 — Start Redis (Standalone)

```bash
docker run -d --name redis-demo -p 6379:6379 redis:7 \
  redis-server --bind 0.0.0.0 --protected-mode no \
  --maxmemory 50mb --maxmemory-policy allkeys-lru
```

**Verify it's running:**

```bash
docker ps
docker exec -it redis-demo redis-cli ping
```

**Expected output:**

```
PONG
```

---

## 🛠️ Step 2 — Build the Project

```bash
mvn clean install
```

**Expected output:**

```
BUILD SUCCESS
```

---

## ▶️ Step 3 — Run the Application

```bash
mvn spring-boot:run
```

The application starts at: **`http://localhost:8080`**

---

## 🌐 Step 4 — Test the Endpoint

```bash
curl -i http://localhost:8080/storage
```

---

## 🧪 Testing Scenarios

### ✅ Healthy — Usage Below Threshold

```json
HTTP/1.1 200

{
  "status": "HEALTHY",
  "storagePercentage": 4
}
```

---

### 🚨 Threshold Exceeded

Load Redis with test data (PowerShell):

```powershell
for ($i = 1; $i -le 20000; $i++) {
    docker exec redis-demo redis-cli SET "bigkey$i" ("x" * 5000)
}
```

Then hit the endpoint:

```json
HTTP/1.1 429

{
  "status": "THRESHOLD_EXCEEDED",
  "storagePercentage": 32
}
```

---

### 🔌 Redis Unavailable

Stop Redis:

```bash
docker stop redis-demo
```

Then hit the endpoint:

```json
HTTP/1.1 503

{
  "status": "REDIS_UNAVAILABLE",
  "storagePercentage": 0
}
```

---

## 🧠 Standalone vs Cluster Parsing Logic

### Standalone Mode

Redis returns flat keys:

```
used_memory: 5000000
maxmemory:   50000000
```

Calculation:

```
utilization = (used_memory / maxmemory) × 100
```

---

### Cluster Mode

Redis returns prefixed keys per node:

```
10.0.0.1:6379.used_memory = 20000000
10.0.0.1:6379.maxmemory   = 50000000
10.0.0.2:6379.used_memory = 45000000
10.0.0.2:6379.maxmemory   = 50000000
```

**Logic:**

1. Extract node prefix from key
2. Group `used_memory` and `maxmemory` per node
3. Calculate utilization per node
4. Return the **maximum** node utilization

---

## 🧪 Unit Tests

Cluster behavior is validated via mocked properties — no real cluster infrastructure needed:

```java
props.setProperty("10.0.0.1:6379.used_memory", "20000000");
props.setProperty("10.0.0.1:6379.maxmemory",   "50000000");
```

**Run all tests:**

```bash
mvn test
```

---

## 🔧 Configuration

The threshold is defined as a constant (default: **10%**):

```java
private static final int THRESHOLD_PERCENT = 10;
```

This can be externalized to `application.properties` if dynamic configuration is needed.

---

## 💡 Engineering Highlights

- Handles both **standalone** and **cluster** Redis deployments
- Defensive exception handling with proper HTTP semantics
- Infrastructure-aware debugging
- Unit-tested cluster parsing logic
- Docker-based production simulation

---

## 🎯 What This Demonstrates

- Distributed system reasoning
- Failure mode handling
- Memory threshold monitoring
- Clean layered architecture
- Test-driven validation
- Production-realistic simulation

---

*Built with ☕ Java 21 + Spring Boot*
