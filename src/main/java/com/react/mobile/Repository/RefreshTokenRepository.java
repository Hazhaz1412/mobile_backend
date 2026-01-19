package com.react.mobile.Repository;

import com.react.mobile.Entity.RefreshToken;
import com.react.mobile.Entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    
    List<RefreshToken> findByUser(AuthUser user);
    
    void deleteByUser(AuthUser user);
}
