package com.atlas.modules.auth.service;

import com.atlas.config.JwtProperties;
import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.exception.UnauthorizedException;
import com.atlas.modules.auth.dto.AuthResponse;
import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.auth.dto.UserResponse;
import com.atlas.modules.auth.entity.RefreshToken;
import com.atlas.modules.auth.entity.User;
import com.atlas.modules.auth.entity.UserStatus;
import com.atlas.modules.auth.mapper.UserMapper;
import com.atlas.modules.auth.repository.RefreshTokenRepository;
import com.atlas.modules.auth.repository.UserRepository;
import com.atlas.modules.auth.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenHasher tokenHasher,
            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenHasher = tokenHasher;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED, "Email is already registered", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is deactivated");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!refreshToken.isActive()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        return issueTokens(refreshToken.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return UserMapper.toResponse(user);
    }

    private LoginResult issueTokens(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());
        String accessToken = jwtService.createAccessToken(principal);

        String rawRefreshToken = tokenHasher.generateRefreshToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHasher.hash(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenExpiration()));
        refreshTokenRepository.save(refreshToken);

        AuthResponse response = new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.accessTokenExpiresInSeconds(),
                jwtService.accessTokenExpiresAt(),
                UserMapper.toResponse(user)
        );
        return new LoginResult(response, rawRefreshToken);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record LoginResult(AuthResponse authResponse, String rawRefreshToken) {
    }
}
