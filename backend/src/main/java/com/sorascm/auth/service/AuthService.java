package com.sorascm.auth.service;

import com.sorascm.auth.domain.RefreshToken;
import com.sorascm.auth.domain.User;
import com.sorascm.auth.dto.AuthDtos.*;
import com.sorascm.auth.repository.RefreshTokenRepository;
import com.sorascm.auth.repository.UserRepository;
import com.sorascm.common.exception.BusinessException;
import com.sorascm.security.UserPrincipal;
import com.sorascm.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final long refreshTokenExpirationDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.security.jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already in use");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already in use");
        }

        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.roles()
        );

        User savedUser = userRepository.save(user);
        UserPrincipal principal = UserPrincipal.fromEntity(savedUser);

        String accessToken = jwtTokenProvider.generateAccessToken(
                savedUser.getId(),
                savedUser.getUsername(),
                principal.getAuthorities()
        );
        String rawRefreshToken = createAndPersistRefreshToken(savedUser);

        return AuthResponse.of(
                accessToken,
                rawRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                toSummary(savedUser)
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new BusinessException("Invalid credentials provided");
        } catch (DisabledException ex) {
            throw new BusinessException("Account is disabled");
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException("User context not found"));

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                principal.getAuthorities()
        );
        String rawRefreshToken = createAndPersistRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                rawRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                toSummary(user)
        );
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (token.isRevoked() || token.isExpired()) {
            throw new BusinessException("Refresh token is expired or revoked");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        if (!user.isActive()) {
            throw new BusinessException("User account is inactive");
        }

        UserPrincipal principal = UserPrincipal.fromEntity(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                principal.getAuthorities()
        );
        String newRawRefreshToken = createAndPersistRefreshToken(user);

        return AuthResponse.of(
                newAccessToken,
                newRawRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                toSummary(user)
        );
    }

    @Transactional
    public void logout(TokenRefreshRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private String createAndPersistRefreshToken(User user) {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing", e);
        }
    }

    private UserSummaryDto toSummary(User user) {
        return new UserSummaryDto(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                user.getRoles()
        );
    }
}