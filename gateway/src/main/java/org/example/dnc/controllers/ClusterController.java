package org.example.dnc.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cluster")
public class ClusterController {

    private final ClusterManager clusterManager;

    public ClusterController(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerNode(@RequestParam String nodeUrl) {
        clusterManager.heartbeatReceived(nodeUrl);
        return ResponseEntity.ok("Heartbeat received");
    }
}
