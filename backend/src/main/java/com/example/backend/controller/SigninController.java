package com.example.backend.controller;

import com.example.backend.auth.AuthService;
import com.example.backend.auth.LoginRequest;
import com.example.backend.auth.LoginResponse;
import com.example.backend.auth.SessionKeys;
import com.example.backend.dto.ModifyUserDTO;
import com.example.backend.dto.UserDTO;
import com.example.backend.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.backend.repository.UserRepository;



@RestController
@RequestMapping("/auth")
public class SigninController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public SigninController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest body, HttpSession session) {
        AuthService.AuthResult result = authService.authenticate(body.getIdentifier(), body.getPassword());

        if (!result.isSuccess()) {
            return ResponseEntity.status(401).body(new LoginResponse(result.getMessage()));
        }

        User user = result.getUser();
        UserDTO dto = new UserDTO(user.getUser_id(), user.getUsername());
        session.setAttribute(SessionKeys.USER_DTO, dto);

        return ResponseEntity.ok(new LoginResponse("Login successful."));
    }

    @PostMapping("/login-and-redirect")
    public ResponseEntity<Void> loginAndRedirect(@RequestBody LoginRequest body, HttpSession session) {
        AuthService.AuthResult result = authService.authenticate(body.getIdentifier(), body.getPassword());

        if (!result.isSuccess()) {
            return ResponseEntity.status(401).build();
        }

        User user = result.getUser();
        UserDTO dto = new UserDTO(user.getUser_id(), user.getUsername());
        session.setAttribute(SessionKeys.USER_DTO, dto);

        return ResponseEntity.status(302).header("Location", "/home").build();
    }



    @GetMapping("/me")
    public ResponseEntity<ModifyUserDTO> getCurrentUser(HttpSession session) {
        UserDTO dto = (UserDTO) session.getAttribute(SessionKeys.USER_DTO);
        if (dto == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        ModifyUserDTO modifyUserDTO = new ModifyUserDTO();
        modifyUserDTO.setId(user.getUser_id());
        modifyUserDTO.setUsername(user.getUsername());
        modifyUserDTO.setEmail(user.getEmail());
        modifyUserDTO.setPhoneNumber(user.getPhone_number());
        modifyUserDTO.setProfilePictureUrl(user.getProfile_picture_url());
        modifyUserDTO.setUpdatedAt(user.getUpdated_at());
        return ResponseEntity.ok(modifyUserDTO);
    }



    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(new LoginResponse("Logged out."));
    }
}
