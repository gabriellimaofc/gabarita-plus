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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email já cadastrado.", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username já cadastrado.", HttpStatus.CONFLICT);
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new BusinessException("Role padrão não configurada.", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().trim().toLowerCase());
        user.setUsername(request.username().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setTargetCourse(request.targetCourse());
        user.setRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);
        log.info("Usuário registrado com sucesso. userId={}, email={}", savedUser.getId(), savedUser.getEmail());
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password())
        );

        User user = userRepository.findByEmail(request.usernameOrEmail())
                .or(() -> userRepository.findByUsername(request.usernameOrEmail()))
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas."));

        log.info("Login realizado com sucesso. userId={}", user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
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
        return new AuthResponse(accessToken, refreshToken.getToken(), "Bearer", jwtService.getAccessTokenExpirationSeconds(), profile);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido."));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
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

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                userMapper.toProfileResponse(user)
        );
    }
}
