package org.example.dnc.service;

import org.example.dnc.entity.UserProfile;
import org.example.dnc.repository.UserProfileRepository;
import org.example.dnc.node.service.CacheNodeService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final CacheNodeService cacheNodeService;
    private final UserProfileRepository userRepository;

    public UserService(CacheNodeService cacheNodeService, UserProfileRepository userRepository) {
        this.cacheNodeService = cacheNodeService;
        this.userRepository = userRepository;
    }

    public void createUser(String userId, String userData) {
        UserProfile newProfile = new UserProfile(userId, userData);
        userRepository.save(newProfile);
        System.out.println("Saved user directly to database: " + userId);
    }

    public String getUserData(String userId) {
        return cacheNodeService.getOrFetch(
                userId,
                300000L, // 5 minutes TTL
                () -> {
                    Optional<UserProfile> user = userRepository.findById(userId);
                    return user.map(UserProfile::getUserData).orElse(null);
                }
        );
    }
}