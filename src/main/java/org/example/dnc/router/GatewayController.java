package org.example.dnc.router;

import jakarta.annotation.PostConstruct;
import org.example.dnc.controllers.ConsistentHashRouter;
import org.example.dnc.entity.UserProfile;
import org.example.dnc.repository.UserProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayController {

    private final ConsistentHashRouter hashRouter;
    private final RestTemplate restTemplate;
    private final UserProfileRepository userRepository;

    public GatewayController(ConsistentHashRouter hashRouter, RestTemplate restTemplate, UserProfileRepository userRepository) {
        this.hashRouter = hashRouter;
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void initCluster() {
        hashRouter.addNode("http://localhost:8081");
        hashRouter.addNode("http://localhost:8082");
        hashRouter.addNode("http://localhost:8083");
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<String> getUser(@PathVariable String id) {
        String targetNodeUrl = hashRouter.getRouteTarget(id);
        String cacheUrl = targetNodeUrl + "/api/v1/cache/" + id;

        System.out.println("➡️ Routing request for '" + id + "' to Node: " + targetNodeUrl);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(cacheUrl, String.class);
            System.out.println("✅ CACHE HIT on Node " + targetNodeUrl);
            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("❌ CACHE MISS on Node " + targetNodeUrl + ". Querying Database...");

            Optional<UserProfile> dbUser = userRepository.findById(id);

            if (dbUser.isPresent()) {
                String dbValue = dbUser.get().getUserData();

                String putUrl = targetNodeUrl + "/api/v1/cache/" + id + "?value=" + dbValue + "&ttlMillis=300000";
                restTemplate.put(putUrl, null);
                System.out.println("💾 Populated Node " + targetNodeUrl + " with DB data.");

                return ResponseEntity.ok(dbValue);
            } else {
                return ResponseEntity.notFound().build();
            }
        }
    }
}