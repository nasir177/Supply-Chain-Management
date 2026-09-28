package com.sorascm.auth.dto;

import com.sorascm.auth.domain.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "Username or email is required")
            String usernameOrEmail,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RegisterRequest(
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50)
            String username,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email address")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters long")
            String password,

            @NotBlank(message = "First name is required")
            String firstName,

            @NotBlank(message = "Last name is required")
            String lastName,

            @NotEmpty(message = "At least one role must be assigned")
            Set<RoleType> roles
    ) {}

    public record TokenRefreshRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            UserSummaryDto user
    ) {
        public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds, UserSummaryDto user) {
            return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
        }
    }

    public record UserSummaryDto(
            String id,
            String username,
            String email,
            String fullName,
            Set<RoleType> roles
    ) {}
}