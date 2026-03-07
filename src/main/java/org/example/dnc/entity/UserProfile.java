package org.example.dnc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profiles") // Good practice to explicitly name your tables
public class UserProfile {

    @Id
    private String userId; // This acts as our primary key AND our cache key

    private String userData; // The actual payload we want to cache

    // JPA requires a no-args constructor
    public UserProfile() {
    }

    public UserProfile(String userId, String userData) {
        this.userId = userId;
        this.userData = userData;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }
}