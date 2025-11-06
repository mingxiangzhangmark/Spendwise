package com.example.backend.controller;

import com.example.backend.auth.SessionKeys;
import com.example.backend.dto.UserDTO;
import com.example.backend.model.UserAchievement;
import com.example.backend.repository.UserAchievementRepository;
import com.example.backend.service.AchievementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/achievements")
public class AchievementController {

    private final AchievementService achievementService;
    private final UserAchievementRepository userAchievementRepository;

    public AchievementController(AchievementService achievementService,
                                 UserAchievementRepository userAchievementRepository) {
        this.achievementService = achievementService;
        this.userAchievementRepository = userAchievementRepository;
    }

    @GetMapping
    public List<UserAchievement> getAchievements(HttpSession session) {

        UserDTO userDTO = (UserDTO) session.getAttribute(SessionKeys.USER_DTO);
        if (userDTO == null) {
            throw new RuntimeException("User not logged-in");
        }

        Integer userId = userDTO.getId();
        return userAchievementRepository.findByUserId(userId);
    }
}
