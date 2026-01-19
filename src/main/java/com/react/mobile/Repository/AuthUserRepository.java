package com.react.mobile.Repository;

import com.react.mobile.Entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
 
    Optional<AuthUser> findByUsername(String username);
    Optional<AuthUser> findByEmail(String email); 
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}