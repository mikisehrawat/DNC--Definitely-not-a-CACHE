# DNC - Distributed Cache Implementation

A Java Spring Boot-based Distributed Cache system built to demonstrate scalable, highly-available caching mechanisms. This project implements core distributed systems concepts such as **Consistent Hashing**, **LRU Eviction Policy**, **Dynamic Node Discovery**, and the **Cache-Aside Pattern**.

---

## 🏗️ High-Level Design (HLD)

The system is decoupled into two primary microservices: the **Gateway** and the **Cache Node**.

### Components
1. **Gateway Service (API Gateway & Router)**
   - Acts as the entry point for all client requests.
   - Manages the **Hash Ring** (using Consistent Hashing) to distribute keys across the available cache nodes.
   - Responsible for fetching data from the persistent storage (Database) on a cache miss, and writing it back to the respective cache node (**Cache-Aside Pattern**).
   - Manages the cluster state by tracking heartbeats from Cache Nodes. If a node fails, it is evicted from the hash ring.

2. **Cache Node Service (Data Node)**
   - Responsible for storing the actual Key-Value pairs in memory.
   - Implements a custom, thread-safe **Least Recently Used (LRU) Cache**.
   - Sends periodic heartbeats (pings) to the Gateway to announce its availability.
   - Supports Time-To-Live (TTL) expiration for cached items.

3. **Database (PostgreSQL)**
   - The persistent source of truth. When data is not in the cache, the Gateway fetches it from here.

### Request Flow
- **GET Request**: Client requests `GET /api/v1/gateway/users/{id}`.
- **Hash Computation**: Gateway hashes the `{id}` using MD5 and finds the target Cache Node on the Hash Ring.
- **Cache Hit**: If the node has the data, it returns it instantly (O(1)).
- **Cache Miss**: Gateway receives a 404, queries the PostgreSQL Database, receives the data, updates the Cache Node via a `PUT` request, and returns the data to the client.

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
   - Discuss how the Gateway prevents the cache nodes from needing direct database access, effectively separating concerns.

---

## 🚀 How to Run the Project

### Prerequisites
- **Java 17+**
- **Maven**
- **PostgreSQL** running locally (or adjust credentials in `gateway/src/main/resources/application.properties`).

### 1. Start the Gateway
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
   - Make the `curl` request again. The Gateway will gracefully re-route the request to the remaining node, fetch it from the DB (cache miss), and populate the new node.
