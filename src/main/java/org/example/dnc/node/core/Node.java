package org.example.dnc.node.core;

public class Node {
    String key;
    String value;
    long expiryTime;
    Node prev;
    Node next;

    public Node(String key, String value, long expiryTime) {
        this.key = key;
        this.value = value;
        this.expiryTime = expiryTime;
    }
}