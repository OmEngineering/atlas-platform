package com.atlas.modules.auth.service;

import com.atlas.config.JwtProperties;
import com.atlas.exception.ApiException;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.auth.entity.User;
import com.atlas.modules.auth.repository.RefreshTokenRepository;
import com.atlas.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenHasher tokenHasher;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                "YXRsYXMtdGVzdC1zZWNyZXQta2V5LTMyLWJ5dGVzISE=",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                "atlas_refresh_token"
        );
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                tokenHasher,
                jwtProperties
        );
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice@example.com", "password1234", "Alice")))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void registerPersistsNormalizedEmail() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1234")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(java.util.UUID.randomUUID());
            user.setCreatedAt(java.time.Instant.now());
            user.setUpdatedAt(java.time.Instant.now());
            return user;
        });

        var response = authService.register(new RegisterRequest("Alice@Example.com", "password1234", "Alice"));

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.emailVerified()).isTrue();
    }
}
