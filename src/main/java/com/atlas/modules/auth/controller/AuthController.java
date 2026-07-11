package com.atlas.modules.auth.controller;

import com.atlas.modules.auth.dto.AuthResponse;
import com.atlas.modules.auth.dto.LoginRequest;
import com.atlas.modules.auth.dto.RegisterRequest;
import com.atlas.modules.auth.dto.UserResponse;
import com.atlas.modules.auth.security.AuthenticatedUser;
import com.atlas.modules.auth.service.AuthService;
import com.atlas.modules.auth.web.RefreshTokenCookieManager;
import com.atlas.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration, login, and session management")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    public AuthController(AuthService authService, RefreshTokenCookieManager refreshTokenCookieManager) {
        this.authService = authService;
        this.refreshTokenCookieManager = refreshTokenCookieManager;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.of(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);
        refreshTokenCookieManager.write(response, result.rawRefreshToken());
        return ApiResponse.of(result.authResponse());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new access token")
    public ApiResponse<AuthResponse> refresh(Cookie[] cookies, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieManager.read(cookies);
        AuthService.LoginResult result = authService.refresh(refreshToken);
        refreshTokenCookieManager.write(response, result.rawRefreshToken());
        return ApiResponse.of(result.authResponse());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke refresh token and clear session cookie")
    public void logout(Cookie[] cookies, HttpServletResponse response) {
        authService.logout(refreshTokenCookieManager.read(cookies));
        refreshTokenCookieManager.clear(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user profile")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.of(authService.getCurrentUser(principal));
    }
}
