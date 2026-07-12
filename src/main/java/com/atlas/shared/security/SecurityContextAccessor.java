package com.atlas.shared.security;

import com.atlas.exception.UnauthorizedException;
import com.atlas.modules.auth.entity.User;
import com.atlas.modules.auth.repository.UserRepository;
import com.atlas.modules.auth.security.AuthenticatedUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityContextAccessor {

    private final UserRepository userRepository;

    public SecurityContextAccessor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthenticatedUser requireAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    public User requireVerifiedUser() {
        AuthenticatedUser principal = requireAuthenticatedUser();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Email verification required");
        }
        if (user.getStatus() != com.atlas.modules.auth.entity.UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is deactivated");
        }
        return user;
    }

    public UUID currentUserId() {
        return requireAuthenticatedUser().id();
    }
}
