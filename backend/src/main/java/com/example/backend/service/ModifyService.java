package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ModifyService {

    private final UserRepository userRepository;

    public ModifyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User updateUser(Integer userId, User updatedData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updatedData.getUsername() != null) user.setUsername(updatedData.getUsername());
        if (updatedData.getEmail() != null) user.setEmail(updatedData.getEmail());
//        if (updatedData.getPhone_number() != null) user.setPhone_number(updatedData.getPhone_number());
        user.setPhone_number(updatedData.getPhone_number());
        if (updatedData.getProfile_picture_url() != null) user.setProfile_picture_url(updatedData.getProfile_picture_url());

        user.setUpdated_at(LocalDateTime.now());

        return userRepository.save(user);
    }
}
