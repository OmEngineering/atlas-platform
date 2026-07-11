package com.atlas.modules.auth.web;

import com.atlas.config.JwtProperties;
import com.atlas.config.RefreshCookieProperties;
import com.atlas.modules.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieManager {

    private final JwtProperties jwtProperties;
    private final RefreshCookieProperties refreshCookieProperties;

    public RefreshTokenCookieManager(JwtProperties jwtProperties, RefreshCookieProperties refreshCookieProperties) {
        this.jwtProperties = jwtProperties;
        this.refreshCookieProperties = refreshCookieProperties;
    }

    public void write(HttpServletResponse response, String rawRefreshToken) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.refreshCookieName(), rawRefreshToken)
                .httpOnly(true)
                .secure(refreshCookieProperties.secure())
                .sameSite(refreshCookieProperties.sameSite())
                .path(refreshCookieProperties.path())
                .maxAge(jwtProperties.refreshTokenExpiration())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.refreshCookieName(), "")
                .httpOnly(true)
                .secure(refreshCookieProperties.secure())
                .sameSite(refreshCookieProperties.sameSite())
                .path(refreshCookieProperties.path())
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String read(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (jwtProperties.refreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
