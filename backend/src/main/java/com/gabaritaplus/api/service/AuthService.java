package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.auth.AuthResponse;
import com.gabaritaplus.api.dto.auth.LoginRequest;
import com.gabaritaplus.api.dto.auth.RefreshTokenRequest;
import com.gabaritaplus.api.dto.auth.RegisterRequest;
import com.gabaritaplus.api.dto.user.UserProfileResponse;
import com.gabaritaplus.api.entity.RefreshToken;
import com.gabaritaplus.api.entity.Role;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.entity.enums.RoleName;
import com.gabaritaplus.api.exception.BusinessException;
import com.gabaritaplus.api.exception.UnauthorizedException;
import com.gabaritaplus.api.mapper.UserMapper;
import com.gabaritaplus.api.repository.RefreshTokenRepository;
import com.gabaritaplus.api.repository.RoleRepository;
import com.gabaritaplus.api.repository.UserRepository;
import com.gabaritaplus.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizePrincipal(request.email());
        String normalizedUsername = normalizePrincipal(request.username());
        log.info("Tentativa de cadastro recebida. email={}, username={}", normalizedEmail, normalizedUsername);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email já cadastrado.", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException("Username já cadastrado.", HttpStatus.CONFLICT);
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new BusinessException("Role padrão não configurada.", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setTargetCourse(normalizeOptionalValue(request.targetCourse()));
        user.setRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);
        log.info("Cadastro concluido com sucesso. userId={}", savedUser.getId());
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String principal = normalizePrincipal(request.usernameOrEmail());
        log.info("Tentativa de login recebida. principal={}", principal);

        User user = userRepository.findByEmail(principal)
                .or(() -> userRepository.findByUsername(principal))
                .orElseThrow(() -> {
                    log.warn("Tentativa de login para usuario inexistente. principal={}", principal);
                    return new UnauthorizedException("Credenciais inválidas.");
                });

        log.info(
                "Usuario localizado para login. userId={}, active={}, roles={}",
                user.getId(),
                user.isActive(),
                user.getRoles().stream().map(role -> role.getName().name()).toList()
        );

        if (!user.isActive()) {
            log.warn("Login bloqueado para usuario inativo. userId={}", user.getId());
            throw new UnauthorizedException("Credenciais inválidas.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Senha invalida para principal={}", principal);
            throw new UnauthorizedException("Credenciais inválidas.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(principal, request.password())
            );
        } catch (BadCredentialsException exception) {
            log.warn("AuthenticationManager recusou credenciais para principal={}", principal);
            throw new UnauthorizedException("Credenciais inválidas.");
        }

        log.info("Login realizado com sucesso. userId={}", user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Tentativa de renovacao de sessao recebida.");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .filter(token -> !token.isRevoked())
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido."));

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token expirado.");
        }

        User user = refreshToken.getUser();
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(role -> role.getName().name()).toArray(String[]::new))
                .build();

        String accessToken = jwtService.generateAccessToken(userDetails);
        UserProfileResponse profile = userMapper.toProfileResponse(user);
        log.info("Sessao renovada com sucesso. userId={}", user.getId());
        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                profile
        );
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        log.info("Tentativa de logout recebida.");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido."));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        log.info("Logout concluido com sucesso. userId={}", refreshToken.getUser().getId());
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(role -> role.getName().name()).toArray(String[]::new))
                .build();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshTokenValue = jwtService.generateRefreshToken(userDetails);

        refreshTokenRepository.deleteByUserId(user.getId());
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenExpirationDays));
        refreshTokenRepository.save(refreshToken);
        log.info(
                "Tokens gerados com sucesso. userId={}, accessTokenExpiresInSeconds={}",
                user.getId(),
                jwtService.getAccessTokenExpirationSeconds()
        );

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                userMapper.toProfileResponse(user)
        );
    }

    private String normalizePrincipal(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
