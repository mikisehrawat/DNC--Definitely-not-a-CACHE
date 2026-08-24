package org.example.dnc.controllers;

import org.example.dnc.controllers.ConsistentHashRouter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final ConsistentHashRouter hashRouter;
    private final RestTemplate restTemplate;

    public UserController(ConsistentHashRouter hashRouter, RestTemplate restTemplate) {
        this.hashRouter = hashRouter;
        this.restTemplate = restTemplate;
    }

    // 1. Route create user to appropriate node
    @PostMapping
    public ResponseEntity<String> createUser(@RequestParam String id, @RequestParam String data) {
        String targetNodeUrl = hashRouter.getRouteTarget(id);
        String createUrl = targetNodeUrl + "/api/v1/users?id=" + id + "&data=" + data;
        
        System.out.println("Routing create user request for '" + id + "' to Node: " + targetNodeUrl);
        ResponseEntity<String> response = restTemplate.postForEntity(createUrl, null, String.class);
        return response;
    }
}