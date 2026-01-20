package com.react.mobile.Repository;

import com.react.mobile.Entity.UserPreferences;
import com.react.mobile.Entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {
    Optional<UserPreferences> findByAuthUser(AuthUser authUser);
    Optional<UserPreferences> findByAuthUserId(Long userId);
}
