# DNC - Distributed Cache Implementation

A Java Spring Boot-based Distributed Cache system built to demonstrate scalable, highly-available caching mechanisms. This project implements core distributed systems concepts such as **Consistent Hashing**, **LRU Eviction Policy**, **Dynamic Node Discovery**, and the **Cache-Aside Pattern**.

---

## 🏗️ High-Level Design (HLD)

The system is decoupled into two primary microservices: the **Gateway** and the **Cache Node**.

### Components
1. **Gateway Service (API Gateway & Router)**
   - Acts as the entry point for all client requests.
   - Manages the **Hash Ring** (using Consistent Hashing) to distribute keys across the available cache nodes.
   - Acts as a pure proxy that routes requests to the appropriate Cache Node without any direct database knowledge.
   - Manages the cluster state by tracking heartbeats from Cache Nodes. If a node fails, it is evicted from the hash ring.

2. **Cache Node Service (Data Node)**
   - Responsible for storing the actual Key-Value pairs in memory.
   - Responsible for fetching data from the persistent storage (Database) on a cache miss, and caching it locally (**Cache-Aside Pattern**).
   - Implements a custom, thread-safe **Least Recently Used (LRU) Cache**.
   - Sends periodic heartbeats (pings) to the Gateway to announce its availability.
   - Supports Time-To-Live (TTL) expiration for cached items.

3. **Database (PostgreSQL)**
   - The persistent source of truth. When data is not in the cache, the Cache Node fetches it from here.

### Request Flow
- **GET Request**: Client requests `GET /api/v1/gateway/users/{id}`.
- **Hash Computation**: Gateway hashes the `{id}` using MD5 and finds the target Cache Node on the Hash Ring.
- **Cache Hit**: If the node has the data, it returns it instantly (O(1)).
- **Cache Miss**: The Cache Node checks its memory, finds nothing, queries the PostgreSQL Database, caches the data locally, and returns it. The Gateway transparently passes this data to the client.

---

## 🛠️ Low-Level Design (LLD)

### 1. Consistent Hashing Router
- **Data Structure**: Uses a `TreeMap<Integer, String>` to represent the hash ring. This allows for `O(log N)` search time using the `tailMap()` function to find the first node clockwise on the ring.
  - **Understanding the `String`**: The `String` stored in the `TreeMap` represents the physical server's address (e.g., `http://localhost:8081`). This allows the Gateway to instantly know where to route the request once the hash is found.
- **Hash Function**: Uses MD5 to ensure uniform distribution of keys.
- **Handling Virtual and Real Nodes**: To prevent "Hotspotting" and uneven data distribution, each physical (real) Cache Node is mapped to `100` virtual nodes on the ring.
  - **Adding a Node**: When a real node (`http://localhost:8081`) joins, we loop 100 times, appending a counter suffix (e.g., `-VN0`, `-VN1`). We hash these suffixed virtual names to get their position on the ring (the `Integer` key), but we store the original real node's address as the `String` value. Thus, any of the 100 virtual hashes naturally point back to the real server.
  - **Removing a Node**: When a real node dies, we simply execute the same loop, recreate the 100 virtual node hashes, and remove them from the `TreeMap` to cleanly detach all its references.

### 2. Thread-Safe LRU Cache
- **Data Structure**: `ConcurrentHashMap` for `O(1)` retrieval paired with a **Doubly Linked List** to track usage order (Least Recently Used at the tail, Most Recently Used at the head).
- **Concurrency Strategy**: `ConcurrentHashMap` handles thread-safe reads natively. A `ReentrantLock` is used strictly around the Doubly Linked List mutations. This avoids synchronizing the entire `get()` and `put()` methods, maximizing throughput while maintaining correctness.
- **TTL Eviction**: A low-priority background thread (`ScheduledExecutorService`) runs every 5 seconds to lazily clean up expired keys in `O(N)` time, similar to how Redis samples and expires keys.

### 3. Dynamic Node Discovery (Heartbeats)
- Cache nodes use a `@Scheduled` task to `POST` a heartbeat to the Gateway every 5 seconds.
- The Gateway maintains a `ConcurrentHashMap<String, Long>` tracking the last timestamp a node was seen.
- A background task on the Gateway runs every 5 seconds, evicting any node that hasn't sent a heartbeat in the last **15 seconds** from the Hash Ring.

---

## 👨‍💻 Interview Talking Points

If discussing this project in a System Design or SDE interview, highlight these aspects:

1. **Why Consistent Hashing?**
   - Standard modulo hashing (`hash(key) % N`) causes massive cache invalidation (data reshuffling) when a node is added or removed. Consistent hashing ensures that only `k/N` keys are remapped, providing high availability.
2. **Why Virtual Nodes?**
   - Without virtual nodes, key distribution on the ring can be uneven. Virtual nodes interleave the nodes on the ring, ensuring a near-perfect uniform distribution of load.
3. **Concurrency in the LRU Cache**:
   - Mention why you didn't just use `Collections.synchronizedMap()`. Using fine-grained locking (`ReentrantLock`) only on the Linked List manipulation prevents read-heavy workloads from blocking each other.
4. **Cache Miss Strategy (Cache-Aside)**:
   - Discuss how the Cache Node manages both the in-memory LRU cache and persistent storage fallback, ensuring the Gateway remains a lightweight, stateless router.

---

## 🚀 How to Run the Project

### 1. Database Setup (PostgreSQL)
Ensure your PostgreSQL database is running and has the following table created:
```sql
CREATE TABLE "Cache" (
    user_id VARCHAR(255) PRIMARY KEY,
    user_data TEXT
);
```

### 2. Running with Docker (Recommended)
You can effortlessly run the entire decoupled cluster without installing Java or Maven using Docker. 

#### A. Starting the Cluster

To start the Gateway service exclusively:
```bash
docker-compose -f docker-compose.shared.yml up -d --no-deps gateway
```

To start individual Cache Node instances, use the following commands. Note that we set the advertised host to `host.docker.internal` so the Gateway can route to them successfully across Docker networks:

**Node 1 (Port 8081):**
```bash
# In PowerShell:
$env:NODE_ADVERTISED_HOST="host.docker.internal"; docker-compose -f docker-compose.shared.yml run -d -p 8081:8081 cachenode
```

**Node 2 (Port 8082):**
```bash
# In PowerShell:
$env:NODE_ADVERTISED_HOST="host.docker.internal"; docker-compose -f docker-compose.shared.yml run -d -p 8082:8082 -e PORT="8082" cachenode
```

#### B. Setting Environment Variables
You can customize the cluster by editing the `environment:` sections inside the `docker-compose.shared.yml` file before running it, or by passing them directly in your terminal. 
For example, to configure the Cache Node's database connection dynamically:
```bash
DB_HOST=192.168.1.10 DB_USER=myuser DB_PASSWORD=mypass docker-compose -f docker-compose.shared.yml up -d
```

#### C. Adding Individual Nodes (Scaling)
Because the Cache Nodes are completely stateless, you can spin up more nodes instantly. Docker will automatically network them, and the Gateway will discover them via heartbeats.
```bash
# Scale up to 3 Cache Nodes instantly!
docker-compose -f docker-compose.shared.yml up -d --scale cachenode=3
```

#### D. Stopping a Particular Node
To test fault tolerance (or perform maintenance), you can kill a specific node. 
First, list your running containers to find the ID of the node you want to stop:
```bash
docker ps
```
Then, gracefully stop it:
```bash
docker stop <container_id>
```
The Gateway will detect the missed heartbeats within 15 seconds and automatically evict the node from the Hash Ring, gracefully routing traffic to the remaining healthy nodes!

### 3. Running from Source (Local Development)

#### Prerequisites
- **Java 17+**
- **Maven**
- **PostgreSQL** running locally (or adjust credentials in `cachenode/src/main/resources/application.properties`).

#### Start the Gateway
Open a terminal and navigate to the `gateway` directory:
```bash
cd gateway
mvn spring-boot:run
```
*The Gateway will start on `http://localhost:8080`.*

### 2. Start Cache Nodes
Open new terminal windows for each Cache Node instance you want to start. Navigate to the `cachenode` directory.

**Node 1:**
```bash
cd cachenode
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

**Node 2:**
```bash
cd cachenode
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

*You will see the Gateway log that new nodes have been detected as they send their heartbeats.*

### 3. Test the Cluster

1. **Populate Data**: (If you have a POST endpoint in UserController, use it, or manually insert into your DB).
2. **Fetch Data**: 
   ```bash
   curl http://localhost:8080/api/v1/gateway/users/1
   ```
3. **Test Fault Tolerance**:
   - Kill one of the Cache Node terminals (`Ctrl+C`).
   - Wait 15 seconds.
   - You will see the Gateway log: `Node evicted due to missed heartbeats`.
   - Make the `curl` request again. The Gateway will gracefully re-route the request to the remaining node, which will fetch it from the DB (cache miss) and cache it.
