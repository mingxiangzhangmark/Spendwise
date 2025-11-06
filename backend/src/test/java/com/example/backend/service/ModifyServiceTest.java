package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ModifyServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ModifyService modifyService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        existingUser = new User();
        existingUser.setUser_id(1);
        existingUser.setUsername("oldName");
        existingUser.setEmail("old@email.com");
        existingUser.setPhone_number("000");
        existingUser.setProfile_picture_url("old.png");
    }

    @Test
    void testUpdateUser_allFieldsNonNull() {
        User update = new User();
        update.setUsername("newName");
        update.setEmail("new@email.com");
        update.setPhone_number("12345");
        update.setProfile_picture_url("new.png");

        when(userRepository.findById(1)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = modifyService.updateUser(1, update);

        assertEquals("newName", result.getUsername());
        assertEquals("new@email.com", result.getEmail());
        assertEquals("12345", result.getPhone_number());
        assertEquals("new.png", result.getProfile_picture_url());
        assertNotNull(result.getUpdated_at());

        verify(userRepository).findById(1);
        verify(userRepository).save(existingUser);
    }

    @Test
    void testUpdateUser_someFieldsNull() {
        User update = new User();
        update.setUsername(null);
        update.setEmail(null);
        update.setPhone_number(null);
        update.setProfile_picture_url(null);

        when(userRepository.findById(1)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User result = modifyService.updateUser(1, update);

        assertEquals("oldName", result.getUsername());
        assertEquals("old@email.com", result.getEmail());
        assertNull(result.getPhone_number());
        assertEquals("old.png", result.getProfile_picture_url());
        assertNotNull(result.getUpdated_at());
    }

    @Test
    void testUpdateUser_userNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());
        User update = new User();
        assertThrows(RuntimeException.class, () -> modifyService.updateUser(1, update));
        verify(userRepository, never()).save(any());
    }
}
