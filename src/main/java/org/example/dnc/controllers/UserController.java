package org.example.dnc.controller;

import org.example.dnc.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 1. Create a user (Saves to DB only)
    @PostMapping
    public ResponseEntity<String> createUser(@RequestParam String id, @RequestParam String data) {
        userService.createUser(id, data);
        return ResponseEntity.ok("User created successfully in database!");
    }

    // 2. Get a user (Triggers the Cache-Aside logic)
    @GetMapping("/{id}")
    public ResponseEntity<String> getUser(@PathVariable String id) {
        String data = userService.getUserData(id);

        if (data == null) {
            return ResponseEntity.notFound().build(); // 404 if not in Cache AND not in DB
        }

        return ResponseEntity.ok(data); // 200 OK
    }
}