package com.react.mobile.Repository;

import com.react.mobile.Entity.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
 
    Optional<AuthUser> findByUsername(String username);
    Optional<AuthUser> findByEmail(String email); 
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    long countByDateJoinedAfter(LocalDateTime from);

    @Query("SELECT u FROM AuthUser u WHERE " +
            "(:search = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
            " OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.dateJoined DESC")
    Page<AuthUser> searchUsers(@Param("search") String search, Pageable pageable);
}
