package com.sharemyrecipe.service;

import com.sharemyrecipe.domain.*;
import com.sharemyrecipe.dto.*;
import com.sharemyrecipe.exception.*;
import com.sharemyrecipe.repository.*;
import com.sharemyrecipe.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${app.email-verification-enabled:false}")
    private boolean emailVerificationEnabled;

    @Transactional
    public UserResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use");
        }
        if (userRepository.existsByHandle(request.handle())) {
            throw new ConflictException("Handle already taken");
        }

        Role role = parseRole(request.role());

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .handle(request.handle().toLowerCase().trim())
                .displayName(request.displayName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .emailVerified(!emailVerificationEnabled)
                .build();

        userRepository.save(user);
        log.info("New user registered: {} ({})", user.getHandle(), user.getRole());
        return UserResponse.from(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return buildTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isTokenValid(request.refreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String tokenHash = hashToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token revoked or expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildTokenPair(stored.getUser());
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private AuthResponse buildTokenPair(User user) {
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTtlMs()))
                .build();
        refreshTokenRepository.save(entity);

        return AuthResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessTtlMs());
    }

    private Role parseRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) return Role.USER;
        try {
            Role r = Role.valueOf(roleStr.toUpperCase());
            // Prevent self-promotion to ADMIN via sign-up
            return r == Role.ADMIN ? Role.USER : r;
        } catch (IllegalArgumentException e) {
            return Role.USER;
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
