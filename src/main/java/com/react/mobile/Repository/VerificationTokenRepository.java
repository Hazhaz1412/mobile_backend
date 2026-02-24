package com.react.mobile.Repository;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.Enums.TokenType;
import com.react.mobile.Entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
     
    VerificationToken findByToken(String token);

    Optional<VerificationToken> findByTokenAndType(String token, TokenType type);

    Optional<VerificationToken> findTopByUserAndTypeAndConfirmedAtIsNullOrderByCreatedAtDesc(
            AuthUser user,
            TokenType type
    );

    Optional<VerificationToken> findTopByUserAndTypeAndOtpCodeAndConfirmedAtIsNullOrderByCreatedAtDesc(
            AuthUser user,
            TokenType type,
            String otpCode
    );

    void deleteByUserAndType(AuthUser user, TokenType type);

    void deleteByUser(AuthUser user);
}
