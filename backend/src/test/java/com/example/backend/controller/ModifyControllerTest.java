package com.example.backend.controller;

import com.example.backend.dto.ModifyUserDTO;
import com.example.backend.dto.UserDTO;
import com.example.backend.model.User;
import com.example.backend.service.ModifyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModifyControllerTest {

    @Mock
    private ModifyService modifyService;

    private ModifyController controller;

    @BeforeEach
    void init() {
        controller = new ModifyController(modifyService);
    }

    @AfterEach
    void cleanUploads() throws IOException {
        Path uploads = Paths.get("uploads");
        if (Files.exists(uploads)) {
            try (var paths = Files.walk(uploads)) {
                paths.sorted((a, b) -> b.compareTo(a)) // delete files before directories
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                // best-effort cleanup
                            }
                        });
            }
        }
    }

    @Test
    void updateUserReturnsDtoFromServiceResult() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER", new UserDTO(5, "carol"));

        User incoming = new User();
        User saved = new User();
        saved.setUser_id(5);
        saved.setUsername("carol");
        saved.setEmail("carol@example.com");
        saved.setPhone_number("123");
        saved.setProfile_picture_url("http://localhost/picture.png");
        saved.setUpdated_at(LocalDateTime.now());

        when(modifyService.updateUser(5, incoming)).thenReturn(saved);

        ResponseEntity<ModifyUserDTO> response = controller.updateUser(session, incoming);

        assertThat(response.getBody()).satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(5);
            assertThat(dto.getUsername()).isEqualTo("carol");
            assertThat(dto.getEmail()).isEqualTo("carol@example.com");
            assertThat(dto.getPhoneNumber()).isEqualTo("123");
            assertThat(dto.getProfilePictureUrl()).isEqualTo("http://localhost/picture.png");
        });
    }

    @Test
    void uploadAvatarSavesFileAndCallsUpdate() throws IOException {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER", new UserDTO(9, "lisa"));

        MockMultipartFile file = new MockMultipartFile(
                "picture_file", "avatar.png", "image/png", "demo".getBytes()
        );

        User updated = new User();
        updated.setUser_id(9);
        updated.setUsername("lisa");
        updated.setEmail("lisa@example.com");
        updated.setProfile_picture_url("http://localhost:8080/api/picture/user_9_avatar.png");
        updated.setUpdated_at(LocalDateTime.now());

        when(modifyService.updateUser(eq(9), any(User.class))).thenReturn(updated);

        ResponseEntity<ModifyUserDTO> response = controller.uploadAvatar(session, file);

        Path expectedPath = Paths.get("uploads/picture/user_9_avatar.png");
        assertThat(Files.exists(expectedPath)).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(modifyService).updateUser(eq(9), captor.capture());
        assertThat(captor.getValue().getProfile_picture_url())
                .isEqualTo("http://localhost:8080/api/picture/user_9_avatar.png");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProfilePictureUrl())
                .isEqualTo("http://localhost:8080/api/picture/user_9_avatar.png");
    }
}
