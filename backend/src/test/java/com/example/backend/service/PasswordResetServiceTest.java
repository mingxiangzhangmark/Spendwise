// java
package com.example.backend.service;

import com.example.backend.dto.ResetPasswordConfirmDTO;
import com.example.backend.dto.SecurityQuestionResponseDTO;
import com.example.backend.model.User;
import com.example.backend.model.SecurityQuestion;
import com.example.backend.model.UserSecurityAnswer;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserSecurityAnswerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSecurityAnswerRepository answerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // getSecurityQuestion - success
    @Test
    void testGetSecurityQuestion_success() {
        String identifier = "user1";
        User user = mock(User.class);
        when(userRepository.findByUsernameOrEmail(identifier, identifier)).thenReturn(user);

        SecurityQuestion question = mock(SecurityQuestion.class);
        when(question.getId()).thenReturn(42);
        when(question.getQuestionText()).thenReturn("fav color?");

        UserSecurityAnswer answer = mock(UserSecurityAnswer.class);
        when(answer.getQuestion()).thenReturn(question);

        when(answerRepository.findByUser(user)).thenReturn(answer);

        SecurityQuestionResponseDTO dto = passwordResetService.getSecurityQuestion(identifier);
        assertNotNull(dto);
        // verify repository interactions
        verify(userRepository, times(1)).findByUsernameOrEmail(identifier, identifier);
        verify(answerRepository, times(1)).findByUser(user);
    }

    // getSecurityQuestion - user not found
    @Test
    void testGetSecurityQuestion_userNotFound() {
        when(userRepository.findByUsernameOrEmail("nope", "nope")).thenReturn(null);
        assertThrows(EntityNotFoundException.class,
                () -> passwordResetService.getSecurityQuestion("nope"));
    }

    // getSecurityQuestion - question not configured
    @Test
    void testGetSecurityQuestion_questionNotConfigured() {
        User user = mock(User.class);
        when(userRepository.findByUsernameOrEmail("u", "u")).thenReturn(user);
        when(answerRepository.findByUser(user)).thenReturn(null);
        assertThrows(EntityNotFoundException.class,
                () -> passwordResetService.getSecurityQuestion("u"));
    }

    // resetPassword - success
    @Test
    void testResetPassword_success() {
        ResetPasswordConfirmDTO dto = mock(ResetPasswordConfirmDTO.class);
        when(dto.getIdentifier()).thenReturn("id");
        when(dto.getQuestionId()).thenReturn(7);
        when(dto.getAnswer()).thenReturn("ans");
        when(dto.getNewPassword()).thenReturn("newPwd");

        User user = mock(User.class);
        when(userRepository.findByUsernameOrEmail("id", "id")).thenReturn(user);

        UserSecurityAnswer stored = mock(UserSecurityAnswer.class);
        when(answerRepository.findByUserAndQuestionId(user, 7)).thenReturn(stored);
        when(stored.getAnswer_hash()).thenReturn("hash");

        when(passwordEncoder.matches("ans", "hash")).thenReturn(true);
        when(passwordEncoder.encode("newPwd")).thenReturn("encodedNewPwd");

        boolean result = passwordResetService.resetPassword(dto);
        assertTrue(result);
        verify(user, times(1)).setPassword_hash("encodedNewPwd");
        verify(userRepository, times(1)).save(user);
    }

    // resetPassword - user not found
    @Test
    void testResetPassword_userNotFound_returnsFalse() {
        ResetPasswordConfirmDTO dto = mock(ResetPasswordConfirmDTO.class);
        when(dto.getIdentifier()).thenReturn("missing");
        when(userRepository.findByUsernameOrEmail("missing", "missing")).thenReturn(null);
        assertFalse(passwordResetService.resetPassword(dto));
    }

    // resetPassword - stored answer null
    @Test
    void testResetPassword_storedAnswerNull_returnsFalse() {
        ResetPasswordConfirmDTO dto = mock(ResetPasswordConfirmDTO.class);
        when(dto.getIdentifier()).thenReturn("id2");
        when(dto.getQuestionId()).thenReturn(1);

        User user = mock(User.class);
        when(userRepository.findByUsernameOrEmail("id2", "id2")).thenReturn(user);
        when(answerRepository.findByUserAndQuestionId(user, 1)).thenReturn(null);

        assertFalse(passwordResetService.resetPassword(dto));
    }

    // resetPassword - answer mismatch
    @Test
    void testResetPassword_answerMismatch_returnsFalse() {
        ResetPasswordConfirmDTO dto = mock(ResetPasswordConfirmDTO.class);
        when(dto.getIdentifier()).thenReturn("id3");
        when(dto.getQuestionId()).thenReturn(2);
        when(dto.getAnswer()).thenReturn("wrong");

        User user = mock(User.class);
        when(userRepository.findByUsernameOrEmail("id3", "id3")).thenReturn(user);

        UserSecurityAnswer stored = mock(UserSecurityAnswer.class);
        when(answerRepository.findByUserAndQuestionId(user, 2)).thenReturn(stored);
        when(stored.getAnswer_hash()).thenReturn("storedHash");

        when(passwordEncoder.matches("wrong", "storedHash")).thenReturn(false);

        assertFalse(passwordResetService.resetPassword(dto));
    }

    // changePassword - success
    @Test
    void testChangePassword_success() {
        Integer userId = 11;
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getPassword_hash()).thenReturn("curHash");
        when(passwordEncoder.matches("current", "curHash")).thenReturn(true);
        when(passwordEncoder.encode("newOne")).thenReturn("encNew");

        boolean result = passwordResetService.changePassword(userId, "current", "newOne");
        assertTrue(result);
        verify(user, times(1)).setPassword_hash("encNew");
        verify(userRepository, times(1)).save(user);
    }

    // changePassword - wrong current password
    @Test
    void testChangePassword_wrongCurrent_returnsFalse() {
        Integer userId = 12;
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getPassword_hash()).thenReturn("h");
        when(passwordEncoder.matches("bad", "h")).thenReturn(false);

        boolean result = passwordResetService.changePassword(userId, "bad", "new");
        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    // changePassword - user not found (throws inside, caught -> false)
    @Test
    void testChangePassword_userNotFound_returnsFalse() {
        Integer userId = 13;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        boolean result = passwordResetService.changePassword(userId, "x", "y");
        assertFalse(result);
    }

    // changePassword - save throws exception -> returns false (covered catch block)
    @Test
    void testChangePassword_saveThrowsException_returnsFalse() {
        Integer userId = 14;
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getPassword_hash()).thenReturn("hash2");
        when(passwordEncoder.matches("ok", "hash2")).thenReturn(true);
        when(passwordEncoder.encode("n")).thenReturn("enc");
        doThrow(new RuntimeException("db")).when(userRepository).save(user);

        boolean result = passwordResetService.changePassword(userId, "ok", "n");
        assertFalse(result);
    }
}