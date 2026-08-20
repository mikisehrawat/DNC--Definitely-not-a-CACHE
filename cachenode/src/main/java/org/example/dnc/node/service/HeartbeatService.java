package org.example.dnc.node.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Service
public class HeartbeatService {

    private final RestTemplate restTemplate;
    
    @Value("${gateway.url}")
    private String gatewayUrl;

    @Value("${server.port}")
    private String serverPort;

    public HeartbeatService() {
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        String nodeUrl = "http://localhost:" + serverPort;
        String registerUrl = gatewayUrl + "/api/v1/cluster/register?nodeUrl=" + nodeUrl;

        try {
            restTemplate.postForEntity(registerUrl, null, String.class);
            // System.out.println("Heartbeat sent to gateway: " + registerUrl);
        } catch (RestClientException e) {
            System.err.println("Failed to send heartbeat to gateway: " + e.getMessage());
        }
    }
}
