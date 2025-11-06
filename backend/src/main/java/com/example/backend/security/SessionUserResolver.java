package com.example.backend.security;

import com.example.backend.auth.SessionKeys;
import com.example.backend.dto.UserDTO;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionUserResolver {

    private final UserRepository userRepository;

    public SessionUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser(HttpSession session) {
        UserDTO dto = (UserDTO) session.getAttribute(SessionKeys.USER_DTO);
        if (dto == null) {
            throw new IllegalStateException("User not logged in.");
        }
        return userRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found."));
    }
}
