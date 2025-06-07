package com.jewelry_ecom_platform.auth_service.repository;

import com.jewelry_ecom_platform.auth_service.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByAccessTokenAndActiveTrue(String accessToken);
    Optional<UserSession> findByRefreshTokenAndActiveTrue(String refreshToken);

    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.email = :email AND s.active = true")
    void deactivateAllSessionsByEmail(String email);

    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.accessToken = :token")
    void deactivateSessionByToken(String token);
}
