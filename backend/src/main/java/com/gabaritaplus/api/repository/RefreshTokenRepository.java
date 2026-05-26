package com.gabaritaplus.api.repository;

import com.gabaritaplus.api.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshToken refreshToken where refreshToken.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    void deleteByExpiresAtBefore(OffsetDateTime now);
}
