package com.example.backend.controller;

import com.example.backend.auth.AuthService;
import com.example.backend.auth.LoginRequest;
import com.example.backend.auth.LoginResponse;
import com.example.backend.auth.SessionKeys;
import com.example.backend.dto.ModifyUserDTO;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SigninControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    private SigninController controller;

    @BeforeEach
    void setUp() {
        controller = new SigninController(authService, userRepository);
    }

    @Test
    void loginSuccessStoresUserInSession() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("bob");
        request.setPassword("pw");

        User user = new User();
        user.setUser_id(33);
        user.setUsername("bob");

        when(authService.authenticate("bob", "pw"))
                .thenReturn(AuthService.AuthResult.success(user));

        MockHttpSession session = new MockHttpSession();

        ResponseEntity<LoginResponse> response = controller.login(request, session);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("successful");
        assertThat(session.getAttribute(SessionKeys.USER_DTO))
                .isNotNull();
    }

    @Test
    void loginFailureReturnsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("bob");
        request.setPassword("bad");

        when(authService.authenticate("bob", "bad"))
                .thenReturn(AuthService.AuthResult.failure("Invalid"));

        MockHttpSession session = new MockHttpSession();

        ResponseEntity<LoginResponse> response = controller.login(request, session);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Invalid");
    }

    @Test
    void loginAndRedirectSuccess() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("alice");
        request.setPassword("pw");

        User user = new User();
        user.setUser_id(11);
        user.setUsername("alice");

        when(authService.authenticate("alice", "pw"))
                .thenReturn(AuthService.AuthResult.success(user));

        MockHttpSession session = new MockHttpSession();

        ResponseEntity<Void> response = controller.loginAndRedirect(request, session);

        assertThat(response.getStatusCodeValue()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/home");
        assertThat(session.getAttribute(SessionKeys.USER_DTO)).isNotNull();
    }

    @Test
    void loginAndRedirectFailure() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("alice");
        request.setPassword("bad");

        when(authService.authenticate("alice", "bad"))
                .thenReturn(AuthService.AuthResult.failure("nope"));

        MockHttpSession session = new MockHttpSession();

        ResponseEntity<Void> response = controller.loginAndRedirect(request, session);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
        assertThat(session.getAttribute(SessionKeys.USER_DTO)).isNull();
    }

    @Test
    void getCurrentUserRequiresSession() {
        MockHttpSession session = new MockHttpSession();

        ResponseEntity<ModifyUserDTO> response = controller.getCurrentUser(session);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
    }

    @Test
    void getCurrentUserReturnsDtoWhenLoggedIn() {
        User user = new User();
        user.setUser_id(77);
        user.setUsername("delta");
        user.setEmail("delta@example.com");
        user.setPhone_number("555");
        user.setProfile_picture_url("pic");
        user.setUpdated_at(LocalDateTime.now());

        when(userRepository.findById(77)).thenReturn(Optional.of(user));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.USER_DTO, new com.example.backend.dto.UserDTO(77, "delta"));

        ResponseEntity<ModifyUserDTO> response = controller.getCurrentUser(session);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("delta");
    }

    @Test
    void logoutInvalidatesSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("a", "b");

        ResponseEntity<LoginResponse> response = controller.logout(session);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(session.isInvalid()).isTrue();
    }
}
