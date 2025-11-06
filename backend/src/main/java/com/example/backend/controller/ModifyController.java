package com.example.backend.controller;

import com.example.backend.dto.ModifyUserDTO;
import com.example.backend.dto.UserDTO;
import com.example.backend.model.User;
import com.example.backend.service.ModifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;


@RestController
@RequestMapping("/users")
public class ModifyController {

    private final ModifyService modifyService;

    public ModifyController(ModifyService modifyService) {
        this.modifyService = modifyService;
    }

    @PutMapping("/myself")
    public ResponseEntity<ModifyUserDTO> updateUser(
            HttpSession session,
            @RequestBody User updatedUser) {
        //{
        //    "id": 6,
        //    "username": "bob",
        //    "email": "bob@example.com",
        //    "phoneNumber": null,
        //    "profilePictureUrl": "http://localhost:8080/picture/user_6_275850_1.jpg",
        //    "updatedAt": "2025-09-22T15:08:21.755413"
        //}
        UserDTO userSession = (UserDTO) session.getAttribute("USER");
        Integer userId = userSession.getId();
        User user = modifyService.updateUser(userId, updatedUser);
        ModifyUserDTO userDTO = toDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/myself/picture")
    public ResponseEntity<ModifyUserDTO> uploadAvatar(
            HttpSession session,
            @RequestParam("picture_file") MultipartFile file) throws IOException {
        //{
        //    "id": 6,
        //    "username": "bob",
        //    "email": "bob@example.com",
        //    "phoneNumber": null,
        //    "profilePictureUrl": "http://localhost:8080/picture/user_6_275850_1.jpg",
        //    "updatedAt": "2025-09-22T15:08:21.755413"
        //}

        UserDTO userSession = (UserDTO) session.getAttribute("USER");
        Integer id = userSession.getId();
        String fileName = "user_" + id + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get("uploads/picture");
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "http://localhost:8080/api/picture/" + fileName;

        User updatedData = new User();
        updatedData.setProfile_picture_url(fileUrl);
        updatedData.setUpdated_at(LocalDateTime.now());

        User updatedUser = modifyService.updateUser(id, updatedData);
        ModifyUserDTO userDTO = toDTO(updatedUser);

        return ResponseEntity.ok(userDTO);
    }

    private ModifyUserDTO toDTO(User user) {
        ModifyUserDTO dto = new ModifyUserDTO();
        dto.setId(user.getUser_id());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhone_number());
        dto.setProfilePictureUrl(user.getProfile_picture_url());
        dto.setUpdatedAt(user.getUpdated_at());
        return dto;
    }
}
