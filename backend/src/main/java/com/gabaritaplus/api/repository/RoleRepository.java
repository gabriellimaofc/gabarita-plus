package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.Role;
import com.gabaritaplus.api.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
