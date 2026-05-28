package com.gabaritaplus.api.service;

import com.gabaritaplus.api.entity.Role;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.entity.enums.RoleName;
import com.gabaritaplus.api.repository.RoleRepository;
import com.gabaritaplus.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Seed controlado habilitado. Verificando dados iniciais.");
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> createRole(RoleName.ROLE_USER, "Usuario padrao"));
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> createRole(RoleName.ROLE_ADMIN, "Administrador da plataforma"));

        if (!userRepository.existsByEmail("admin@gabaritaplus.com")) {
            User admin = new User();
            admin.setFullName("Admin Gabarita+");
            admin.setEmail("admin@gabaritaplus.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRoles(Set.of(adminRole, userRole));
            userRepository.save(admin);
            log.info("Usuario seed admin criado.");
        }

        if (!userRepository.existsByEmail("user@gabaritaplus.com")) {
            User user = new User();
            user.setFullName("Aluno Demo");
            user.setEmail("user@gabaritaplus.com");
            user.setUsername("aluno");
            user.setPassword(passwordEncoder.encode("User@123"));
            user.setRoles(Set.of(userRole));
            userRepository.save(user);
            log.info("Usuario seed demo criado.");
        }

        log.info("Seed de questoes desabilitado para evitar conteudo ficticio.");
    }

    private Role createRole(RoleName roleName, String description) {
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(description);
        return roleRepository.save(role);
    }
}
