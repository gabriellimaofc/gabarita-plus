package com.gabaritaplus.api.controller;

import com.gabaritaplus.api.dto.common.ApiResponse;
import com.gabaritaplus.api.dto.user.UpdateUserProfileRequest;
import com.gabaritaplus.api.dto.user.UserProfileResponse;
import com.gabaritaplus.api.dto.user.UserStatisticsResponse;
import com.gabaritaplus.api.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuários")
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> profile() {
        return ResponseEntity.ok(ApiResponse.success("Perfil carregado com sucesso.", userService.getCurrentProfile()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> update(@Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Perfil atualizado com sucesso.", userService.updateProfile(request)));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<UserStatisticsResponse>> statistics() {
        return ResponseEntity.ok(ApiResponse.success("Estatísticas carregadas com sucesso.", userService.getStatistics()));
    }
}
