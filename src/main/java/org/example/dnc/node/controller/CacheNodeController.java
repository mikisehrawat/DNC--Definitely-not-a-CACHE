package org.example.dnc.node.controller;

import org.example.dnc.node.service.CacheNodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cache")
public class CacheNodeController {

    private final CacheNodeService cacheNodeService;

    // Constructor injection is best practice in Spring Boot
    public CacheNodeController(CacheNodeService cacheNodeService) {
        this.cacheNodeService = cacheNodeService;
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> put(@PathVariable String key,
                                    @RequestParam String value,
                                    @RequestParam long ttlMillis) {
        cacheNodeService.put(key, value, ttlMillis);
        System.out.println("Putting key: " + key + " with value: " + value);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<String> get(@PathVariable String key) {
        String value = cacheNodeService.get(key);

        if (value == null) {
            System.out.println("Key not found: " + key);
            return ResponseEntity.notFound().build();
        }
        System.out.println("Getting key: " + key + " with value: " + value);
        return ResponseEntity.ok(value);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        cacheNodeService.delete(key);
        System.out.println("Deleting key: " + key);
        return ResponseEntity.ok().build();
    }
}