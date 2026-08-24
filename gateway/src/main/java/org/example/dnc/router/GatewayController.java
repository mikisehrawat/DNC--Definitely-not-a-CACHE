package org.example.dnc.router;

import org.example.dnc.controllers.ConsistentHashRouter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayController {

    private final ConsistentHashRouter hashRouter;
    private final RestTemplate restTemplate;

    public GatewayController(ConsistentHashRouter hashRouter, RestTemplate restTemplate) {
        this.hashRouter = hashRouter;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<String> getUser(@PathVariable String id) {
        String targetNodeUrl = hashRouter.getRouteTarget(id);
        String cacheUrl = targetNodeUrl + "/api/v1/cache/" + id;

        System.out.println("➡️ Routing request for '" + id + "' to Node: " + targetNodeUrl);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(cacheUrl, String.class);
            System.out.println("✅ CACHE HIT/FETCHED on Node " + targetNodeUrl);
            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("❌ CACHE MISS on Node " + targetNodeUrl + ". Node returned 404.");
            return ResponseEntity.notFound().build();
        }
    }
}