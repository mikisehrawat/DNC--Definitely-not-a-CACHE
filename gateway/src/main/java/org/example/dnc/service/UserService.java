package org.example.dnc.service;

import org.example.dnc.entity.UserProfile;
import org.example.dnc.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserProfileRepository userRepository;

    public UserService(UserProfileRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void createUser(String userId, String userData) {
        UserProfile newProfile = new UserProfile(userId, userData);
        userRepository.save(newProfile);
        System.out.println("Saved user directly to database: " + userId);
    }
}