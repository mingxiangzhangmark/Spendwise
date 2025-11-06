package com.example.backend.controller;

import com.example.backend.dto.ChangePasswordDTO;
import com.example.backend.dto.ResetPasswordConfirmDTO;
import com.example.backend.dto.ResetPasswordRequestDTO;
import com.example.backend.dto.SecurityQuestionResponseDTO;
import com.example.backend.model.User;
import com.example.backend.security.SessionUserResolver;
import com.example.backend.service.PasswordResetService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private SessionUserResolver sessionUserResolver;

    @Mock
    private HttpSession session;

    private PasswordResetController controller;

    @BeforeEach
    void setup() {
        controller = new PasswordResetController(passwordResetService, sessionUserResolver);
    }

    @Test
    void requestQuestionReturnsSecurityQuestion() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setIdentifier("user@example.com");

        SecurityQuestionResponseDTO dto = new SecurityQuestionResponseDTO(1, "Question?");
        when(passwordResetService.getSecurityQuestion("user@example.com")).thenReturn(dto);

        ResponseEntity<SecurityQuestionResponseDTO> response = controller.requestQuestion(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void requestQuestionReturnsNotFoundWhenMissingUser() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setIdentifier("missing@example.com");

        when(passwordResetService.getSecurityQuestion("missing@example.com"))
                .thenThrow(new EntityNotFoundException());

        ResponseEntity<SecurityQuestionResponseDTO> response = controller.requestQuestion(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void resetPasswordSuccess() {
        ResetPasswordConfirmDTO dto = new ResetPasswordConfirmDTO();
        when(passwordResetService.resetPassword(dto)).thenReturn(true);

        ResponseEntity<String> response = controller.resetPassword(dto);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("successful");
    }

    @Test
    void resetPasswordFailure() {
        ResetPasswordConfirmDTO dto = new ResetPasswordConfirmDTO();
        when(passwordResetService.resetPassword(dto)).thenReturn(false);

        ResponseEntity<String> response = controller.resetPassword(dto);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
        assertThat(response.getBody()).contains("incorrect");
    }

    @Test
    void changePasswordSuccess() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("old");
        dto.setNewPassword("new");

        User user = new User();
        user.setUser_id(10);

        when(sessionUserResolver.getCurrentUser(session)).thenReturn(user);
        when(passwordResetService.changePassword(10, "old", "new")).thenReturn(true);

        ResponseEntity<String> response = controller.changePassword(dto, session);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("updated");
    }

    @Test
    void changePasswordUnauthorizedWhenCurrentPasswordWrong() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("wrong");
        dto.setNewPassword("new");

        User user = new User();
        user.setUser_id(12);

        when(sessionUserResolver.getCurrentUser(session)).thenReturn(user);
        when(passwordResetService.changePassword(12, "wrong", "new")).thenReturn(false);

        ResponseEntity<String> response = controller.changePassword(dto, session);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
        assertThat(response.getBody()).contains("incorrect");
    }

    @Test
    void changePasswordWhenUserMissingReturnsUnauthorized() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        doThrow(new IllegalStateException("User not logged in"))
                .when(sessionUserResolver).getCurrentUser(session);

        ResponseEntity<String> response = controller.changePassword(dto, session);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
        assertThat(response.getBody()).contains("not logged in");
    }

    @Test
    void changePasswordUnexpectedErrorReturnsServerError() {
        ChangePasswordDTO dto = new ChangePasswordDTO();

        User user = new User();
        user.setUser_id(13);

        when(sessionUserResolver.getCurrentUser(session)).thenReturn(user);
        when(passwordResetService.changePassword(eq(13), anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<String> response = controller.changePassword(dto, session);

        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody()).contains("An error occurred");
    }
}
