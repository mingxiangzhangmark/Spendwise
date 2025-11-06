package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Data
@Setter
@Getter
@Entity
@Table(name = "users")
public class User {
    // Getters and setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer user_id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password_hash;

    @JsonProperty("phoneNumber")
    private String phone_number;

    private String profile_picture_url;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime created_at;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updated_at;

    private LocalDateTime last_login_at;

    public User(Integer user_id) {
        this.user_id = user_id;
    }

    public User() {}

}
