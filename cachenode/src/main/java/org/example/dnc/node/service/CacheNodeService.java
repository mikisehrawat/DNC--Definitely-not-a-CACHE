package org.example.dnc.node.service;

import org.example.dnc.node.core.LRUCache;
import org.example.dnc.node.entity.UserProfile;
import org.example.dnc.node.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CacheNodeService {

    private final LRUCache localCache = new LRUCache(10000);
    private final UserProfileRepository userRepository;

    public CacheNodeService(UserProfileRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void put(String key, String value, long ttlMillis) {
        localCache.put(key, value, ttlMillis);
    }

    public String get(String key) {
        String value = localCache.get(key);
        if (value != null) {
            return value;
        }

        // Cache miss: query DB
        System.out.println("❌ CACHE MISS on node for key: " + key + ". Querying DB...");
        Optional<UserProfile> dbUser = userRepository.findById(key);
        if (dbUser.isPresent()) {
            String dbValue = dbUser.get().getUserData();
            // Cache it (default 5 mins)
            put(key, dbValue, 300000);
            System.out.println("💾 Fetched from DB and cached key: " + key);
            return dbValue;
        }

        return null;
    }

    public void delete(String key) {
        localCache.remove(key);
    }

    public void createUser(String key, String data) {
        UserProfile newProfile = new UserProfile(key, data);
        userRepository.save(newProfile);
        System.out.println("Saved user directly to database from node: " + key);
        // Optionally put in cache as well
        put(key, data, 300000);
    }
}