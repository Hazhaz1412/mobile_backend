package com.react.mobile.Repository;

import com.react.mobile.Entity.UserProfile;
import com.react.mobile.Entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByAuthUser(AuthUser authUser);
    Optional<UserProfile> findByAuthUserId(Long userId);
}
