// java
package com.example.backend.service;

import com.example.backend.dto.RegisterDTO;
import com.example.backend.model.SecurityQuestion;
import com.example.backend.model.User;
import com.example.backend.model.UserSecurityAnswer;
import com.example.backend.repository.QuestionRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserSecurityAnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RegisterServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserSecurityAnswerRepository userSecurityAnswerRepository;

    @Mock
    private AchievementService achievementService;

    @InjectMocks
    private RegisterService registerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    if (u.getUser_id() == null) u.setUser_id(999);
                    return u;
                });
        when(userSecurityAnswerRepository.save(any(UserSecurityAnswer.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void testRegister_successWithoutSecurityQuestion() {
        RegisterDTO dto = mock(RegisterDTO.class);
        when(dto.getUsername()).thenReturn("userA");
        when(dto.getEmail()).thenReturn("a@example.com");
        when(dto.getPhoneNumber()).thenReturn("123");
        when(dto.getPassword()).thenReturn("plainPwd");
        when(dto.getQuestionId()).thenReturn(null);
        when(dto.getAnswer()).thenReturn(null);

        when(userRepository.findByUsername("userA")).thenReturn(null);
        when(userRepository.findByEmail("a@example.com")).thenReturn(null);
        when(passwordEncoder.encode("plainPwd")).thenReturn("encodedPwd");

        User saved = registerService.register(dto);

        assertNotNull(saved);
        assertEquals("userA", saved.getUsername());
        assertEquals("a@example.com", saved.getEmail());
        assertEquals("encodedPwd", saved.getPassword_hash());
        assertNotNull(saved.getCreated_at());
        assertEquals(saved.getCreated_at(), saved.getUpdated_at());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userSecurityAnswerRepository, never()).save(any());
        verify(achievementService, times(1)).earnIfNotEarned(saved.getUser_id(), "ACCOUNT_CREATED");
    }

    @Test
    void testRegister_withSecurityQuestion_savesAnswer() {
        RegisterDTO dto = mock(RegisterDTO.class);
        when(dto.getUsername()).thenReturn("userB");
        when(dto.getEmail()).thenReturn("b@example.com");
        when(dto.getPhoneNumber()).thenReturn("234");
        when(dto.getPassword()).thenReturn("pwdB");
        when(dto.getQuestionId()).thenReturn(5);
        when(dto.getAnswer()).thenReturn("myAns");

        when(userRepository.findByUsername("userB")).thenReturn(null);
        when(userRepository.findByEmail("b@example.com")).thenReturn(null);
        when(passwordEncoder.encode("pwdB")).thenReturn("encPwdB");
        when(passwordEncoder.encode("myAns")).thenReturn("encAns");
        SecurityQuestion q = new SecurityQuestion();
        q.setId(5);
        when(questionRepository.findById(5)).thenReturn(Optional.of(q));

        ArgumentCaptor<UserSecurityAnswer> captor = ArgumentCaptor.forClass(UserSecurityAnswer.class);
        User saved = registerService.register(dto);

        verify(userSecurityAnswerRepository, times(1)).save(captor.capture());
        UserSecurityAnswer savedAnswer = captor.getValue();
        assertEquals("encAns", savedAnswer.getAnswer_hash());
        assertEquals(saved, savedAnswer.getUser());
        assertEquals(q, savedAnswer.getQuestion());
    }

    @Test
    void testRegister_usernameExists_throws() {
        RegisterDTO dto = mock(RegisterDTO.class);
        when(dto.getUsername()).thenReturn("exists");
        when(dto.getEmail()).thenReturn("x@example.com");
        when(userRepository.findByUsername("exists")).thenReturn(new User());

        assertThrows(RuntimeException.class, () -> registerService.register(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_emailExists_throws() {
        RegisterDTO dto = mock(RegisterDTO.class);
        when(dto.getUsername()).thenReturn("new");
        when(dto.getEmail()).thenReturn("exists@example.com");
        when(userRepository.findByUsername("new")).thenReturn(null);
        when(userRepository.findByEmail("exists@example.com")).thenReturn(new User());

        assertThrows(RuntimeException.class, () -> registerService.register(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_invalidQuestion_throws() {
        RegisterDTO dto = mock(RegisterDTO.class);
        when(dto.getUsername()).thenReturn("userC");
        when(dto.getEmail()).thenReturn("c@example.com");
        when(dto.getPassword()).thenReturn("p");
        when(dto.getQuestionId()).thenReturn(123);
        when(dto.getAnswer()).thenReturn("a");
        when(userRepository.findByUsername("userC")).thenReturn(null);
        when(userRepository.findByEmail("c@example.com")).thenReturn(null);
        when(passwordEncoder.encode("p")).thenReturn("encP");
        when(questionRepository.findById(123)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> registerService.register(dto));
        verify(userSecurityAnswerRepository, never()).save(any());
    }

    @Test
    void testRegister_achievementThrows_butReturnsUser() {
        RegisterDTO dto = mock(RegisterDTO.class);
        when(dto.getUsername()).thenReturn("userD");
        when(dto.getEmail()).thenReturn("d@example.com");
        when(dto.getPhoneNumber()).thenReturn("000");
        when(dto.getPassword()).thenReturn("pwdD");
        when(dto.getQuestionId()).thenReturn(null);
        when(dto.getAnswer()).thenReturn(null);

        when(userRepository.findByUsername("userD")).thenReturn(null);
        when(userRepository.findByEmail("d@example.com")).thenReturn(null);
        when(passwordEncoder.encode("pwdD")).thenReturn("encD");
        doThrow(new RuntimeException("boom")).when(achievementService).earnIfNotEarned(anyInt(), eq("ACCOUNT_CREATED"));

        User saved = registerService.register(dto);

        assertNotNull(saved);
        verify(userRepository, times(1)).save(any());
        verify(achievementService, times(1)).earnIfNotEarned(saved.getUser_id(), "ACCOUNT_CREATED");
    }
}