package com.example.backend.controller;

import com.example.backend.dto.RegisterDTO;
import com.example.backend.dto.UserDTO;
import com.example.backend.model.User;
import com.example.backend.service.RegisterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

    @Mock
    private RegisterService registerService;

    @InjectMocks
    private RegisterController controller;

    @BeforeEach
    void setUp() {
        controller = new RegisterController(registerService);
    }

    @Test
    void registerUserReturnsCreatedResponse() {
        RegisterDTO dto = new RegisterDTO();

        User user = new User();
        user.setUser_id(20);
        user.setUsername("newuser");

        when(registerService.register(dto)).thenReturn(user);

        ResponseEntity<?> response = controller.registerUser(dto);

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getBody()).isInstanceOf(UserDTO.class);
        assertThat(((UserDTO) response.getBody()).getId()).isEqualTo(20);
    }

    @Test
    void registerUserReturnsConflictWhenServiceThrows() {
        RegisterDTO dto = new RegisterDTO();
        when(registerService.register(dto)).thenThrow(new RuntimeException("exists"));

        ResponseEntity<?> response = controller.registerUser(dto);

        assertThat(response.getStatusCodeValue()).isEqualTo(409);
        assertThat(response.getBody()).isEqualTo("exists");
    }
}
