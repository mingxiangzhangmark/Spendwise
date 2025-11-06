package com.example.backend.controller;

import com.example.backend.dto.RegisterDTO;
import com.example.backend.dto.UserDTO;
import com.example.backend.model.User;
import com.example.backend.service.RegisterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping
    public ResponseEntity<?> registerUser(@RequestBody RegisterDTO dto) {
        try {
            User createdUser = registerService.register(dto);
            UserDTO response = new UserDTO(
                createdUser.getUser_id(),
                createdUser.getUsername()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
