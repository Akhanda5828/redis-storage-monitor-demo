Redis Storage Monitor
Author: Akhanda Pal Biswas
📌 Project Overview

This project is a Spring Boot–based Redis Storage Monitoring Service that:

Connects to Redis

Reads memory metrics using INFO memory

Calculates memory utilization percentage

Supports both:

✅ Standalone Redis

✅ Redis Cluster (prefixed node metrics)

Enforces a configurable threshold (10%)

Returns meaningful HTTP status codes

🚀 Features
Scenario	HTTP Code	Description
Redis Healthy & Below Threshold	200 OK	System operating normally
Memory ≥ 10%	429 Too Many Requests	Threshold exceeded
Redis Unreachable	503 Service Unavailable	Dependency failure
🧠 Why This Project?

In production environments:

Some deployments use standalone Redis

Some use Redis Cluster

Cluster mode prefixes memory metrics per node

Original flat-key logic fails in cluster mode

This project solves that problem by:

Detecting deployment type dynamically

Parsing prefixed cluster keys

Calculating per-node memory utilization

Returning the maximum node utilization

Avoiding hotspot masking (no averaging)

🏗 Architecture
Controller  →  Service  →  RedisDatabaseService  →  Redis
Layers

StorageController
Handles HTTP requests and status codes

RedisStorageMonitorService
Business logic (threshold + cluster parsing)

RedisDatabaseService
Retrieves INFO memory from Redis

⚙️ Prerequisites

Make sure you have:

✅ Java 21 (LTS)

✅ Maven

✅ Docker Desktop (Running)

✅ Proxy disabled (if blocking localhost)

🐳 Step 1 — Start Redis (Standalone)
docker run -d --name redis-demo -p 6379:6379 redis:7 `
redis-server --bind 0.0.0.0 --protected-mode no --maxmemory 50mb --maxmemory-policy allkeys-lru

Verify:

docker ps
docker exec -it redis-demo redis-cli ping

Expected:

PONG
🛠 Step 2 — Build Project

Navigate to project root:

mvn clean install

Expected:

BUILD SUCCESS
▶️ Step 3 — Run Application
mvn spring-boot:run

Application runs at:

http://localhost:8080
🌐 Step 4 — Test Endpoint
Show HTTP status + JSON:
curl.exe -i http://localhost:8080/storage
🧪 Testing Different Scenarios
✅ Healthy Scenario

When usage < 10%

Response:

HTTP/1.1 200
{
"status": "HEALTHY",
"storagePercentage": 4
}
🚨 Threshold Exceeded

Insert heavy data:

for ($i = 1; $i -le 20000; $i++) {
docker exec redis-demo redis-cli SET "bigkey$i" ("x" * 5000)
}

Now call endpoint:

HTTP/1.1 429
{
"status": "THRESHOLD_EXCEEDED",
"storagePercentage": 32
}
🔌 Redis Down

Stop Redis:

docker stop redis-demo

Call endpoint:

HTTP/1.1 503
{
"status": "REDIS_UNAVAILABLE",
"storagePercentage": 0
}
🧠 Standalone vs Cluster Logic
Standalone Mode

Redis returns:

used_memory
maxmemory

Service calculates:

(used_memory / maxmemory) * 100
Cluster Mode

Redis may return:

10.0.0.1:6379.used_memory
10.0.0.1:6379.maxmemory
10.0.0.2:6379.used_memory
10.0.0.2:6379.maxmemory

Logic:

Extract node prefix

Group memory per node

Compute percentage per node

Return maximum node utilization

Why Maximum?

Because:

The most saturated shard becomes the bottleneck.
Averaging hides hotspots.

🧪 Unit Tests

Cluster behavior is validated via mocked properties:

props.setProperty("10.0.0.1:6379.used_memory", "20000000");
props.setProperty("10.0.0.1:6379.maxmemory", "50000000");

This ensures cluster logic works without spinning real cluster infrastructure.

Run tests:

mvn test
🔧 Configuration

Threshold (default 10%) defined in:

private static final int THRESHOLD_PERCENT = 10;

Can be externalized to application.properties if needed.

📦 Project Structure
redis-storage-monitor
├── controller/
├── service/
├── test/
├── pom.xml
└── README.md
💡 Engineering Highlights

Handles both standalone & cluster Redis

Defensive exception handling

Proper HTTP semantics (200 / 429 / 503)

Infrastructure-aware debugging

Unit-tested cluster parsing logic

Docker-based production simulation

🎯 What This Demonstrates

Distributed system reasoning

Failure handling

Threshold monitoring

Clean architecture

Test-driven validation

Production realism

👤 Author

Akhanda Pal Biswas