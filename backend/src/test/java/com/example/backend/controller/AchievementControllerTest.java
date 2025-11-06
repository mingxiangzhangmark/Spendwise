package com.example.backend.controller;

import com.example.backend.auth.SessionKeys;
import com.example.backend.dto.UserDTO;
import com.example.backend.model.UserAchievement;
import com.example.backend.repository.UserAchievementRepository;
import com.example.backend.service.AchievementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementControllerTest {

    @Mock
    private AchievementService achievementService;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @InjectMocks
    private AchievementController controller;

    @Test
    void getAchievementsReturnsListForLoggedInUser() {
        MockHttpSession session = new MockHttpSession();
        var dto = new UserDTO(7, "alice");
        session.setAttribute(SessionKeys.USER_DTO, dto);

        var achievement = new UserAchievement();
        List<UserAchievement> expected = List.of(achievement);
        when(userAchievementRepository.findByUserId(7)).thenReturn(expected);

        var result = controller.getAchievements(session);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getAchievementsThrowsWhenUserNotInSession() {
        MockHttpSession session = new MockHttpSession();

        assertThatThrownBy(() -> controller.getAchievements(session))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not logged-in");
    }
}
