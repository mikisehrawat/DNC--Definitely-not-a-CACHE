package org.example.dnc.node.service;

import org.example.dnc.node.core.LRUCache;
import org.springframework.stereotype.Service;

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
}