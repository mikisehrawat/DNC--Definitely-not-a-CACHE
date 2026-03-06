package org.example.dnc.node.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache {
    private final int capacity;
    private final ConcurrentHashMap<String, Node> cache;
    private final Node head;
    private final Node tail;
    //used to make the double linked list single thread why not syncronize ?
    // because that will make the whole code single thread and hence make it slow which we cant affoard
    // ReentrantLock specifcly make the DLL single thread not the remaining code
    // ConcurrentHashMap is by default single thread
    private final ReentrantLock lock = new ReentrantLock();

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>(capacity);

        this.head = new Node("", "", 0L);
        this.tail = new Node("", "", 0L);
        head.next = tail;
        tail.prev = head;


        // This helps to remove the expired data in O(N) time comp using scheduleer
        // it dosent intrupt the regular working because it is a low priority task
        // What redis does is in O(1) time comp because it randomly checks 20 keys and delete the expired one
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::cleanupExpiredKeys, 5, 5, TimeUnit.SECONDS);
    }

    public String get(String key) {
        Node node = cache.get(key);

        if (node == null) {
            return null;
        }

        if (System.currentTimeMillis() > node.expiryTime) {
            remove(key);
            return null;
        }

        lock.lock();
        try {
            removeNode(node);
            insertNodeAtHead(node);
        } finally {
            lock.unlock();
        }

        return node.value;
    }

    public void put(String key, String value, long ttlMillis) {
        long expiryTime = System.currentTimeMillis() + ttlMillis;
        Node node = cache.get(key);

        lock.lock();
        try {
            if (node != null) {
                node.value = value;
                node.expiryTime = expiryTime;
                removeNode(node);
                insertNodeAtHead(node);
            } else {
                if (cache.size() >= capacity) {
                    Node lruNode = tail.prev;
                    cache.remove(lruNode.key);
                    removeNode(lruNode);
                }

                Node newNode = new Node(key, value, expiryTime);
                cache.put(key, newNode);
                insertNodeAtHead(newNode);
            }
        } finally {
            lock.unlock();
        }
    }

    public void remove(String key) {
        Node node = cache.remove(key);
        if (node != null) {
            lock.lock();
            try {
                removeNode(node);
            } finally {
                lock.unlock();
            }
        }
    }

    private void insertNodeAtHead(Node node) {
        node.next = head.next;
        node.next.prev = node;
        head.next = node;
        node.prev = head;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void cleanupExpiredKeys() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Node> entry : cache.entrySet()) {
            if (now > entry.getValue().expiryTime) {
                remove(entry.getKey());
            }
        }
    }
}