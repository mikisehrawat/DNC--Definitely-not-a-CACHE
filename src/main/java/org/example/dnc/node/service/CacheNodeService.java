package org.example.dnc.node.service;

import org.example.dnc.node.core.LRUCache;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class CacheNodeService {

    private final LRUCache localCache = new LRUCache(10000);

    public void put(String key, String value, long ttlMillis) {
        localCache.put(key, value, ttlMillis);
    }

    public String get(String key) {
        return localCache.get(key);
    }

    public void delete(String key) {
        localCache.remove(key);
    }
    public String getOrFetch(String key, long ttlMillis, Supplier<String> databaseFallback) {
        String cachedValue = localCache.get(key);
        if (cachedValue != null) {
            System.out.println("CACHE HIT for key: " + key);
            return cachedValue;
        }
        System.out.println("CACHE MISS. Executing fallback database function for key: " + key);
        String dbValue = databaseFallback.get();

        if (dbValue != null) {
            localCache.put(key, dbValue, ttlMillis);
            System.out.println("Database function executed and value stored in cache for key: " + key);
        }
        else{
            System.out.println("Database function returned null for key: " + key);
        }

        return dbValue;
    }
}