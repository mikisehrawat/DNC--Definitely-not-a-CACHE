package org.example.dnc.controllers;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClusterManager {

    private final ConsistentHashRouter hashRouter;
    private final ConcurrentHashMap<String, Long> nodeHeartbeats = new ConcurrentHashMap<>();
    private static final long EVICTION_THRESHOLD_MS = 15000;

    public ClusterManager(ConsistentHashRouter hashRouter) {
        this.hashRouter = hashRouter;
    }

    public void heartbeatReceived(String nodeUrl) {
        if (!nodeHeartbeats.containsKey(nodeUrl)) {
            System.out.println("New node detected: " + nodeUrl);
            hashRouter.addNode(nodeUrl);
        }
        nodeHeartbeats.put(nodeUrl, System.currentTimeMillis());
    }

    @Scheduled(fixedRate = 5000)
    public void evictDeadNodes() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = nodeHeartbeats.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String nodeUrl = entry.getKey();
            Long lastHeartbeat = entry.getValue();

            if (now - lastHeartbeat > EVICTION_THRESHOLD_MS) {
                System.out.println("Node evicted due to missed heartbeats: " + nodeUrl);
                hashRouter.removeNode(nodeUrl);
                iterator.remove();
            }
        }
    }
}
