package org.example.dnc.controllers;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

@Component
public class ConsistentHashRouter {

    private final SortedMap<Integer, String> hashRing = new TreeMap<>();

    private static final int VIRTUAL_NODES = 100;
    public void addNode(String nodeUrl) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            int hash = getHash(nodeUrl + "-VN" + i);
            hashRing.put(hash, nodeUrl);
        }
        System.out.println("Added Node to cluster: " + nodeUrl);
    }
    public void removeNode(String nodeUrl) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            int hash = getHash(nodeUrl + "-VN" + i);
            hashRing.remove(hash);
        }
        System.out.println("Removed Node from cluster: " + nodeUrl);
    }
    public String getRouteTarget(String cacheKey) {
        if (hashRing.isEmpty()) {
            throw new IllegalStateException("No cache nodes available in the cluster!");
        }

        int hash = getHash(cacheKey);
        SortedMap<Integer, String> tailMap = hashRing.tailMap(hash);

        int nodeHash = tailMap.isEmpty() ? hashRing.firstKey() : tailMap.firstKey();

        return hashRing.get(nodeHash);
    }

    private int getHash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            return ((digest[3] & 0xFF) << 24) |
                    ((digest[2] & 0xFF) << 16) |
                    ((digest[1] & 0xFF) << 8)  |
                    (digest[0] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}