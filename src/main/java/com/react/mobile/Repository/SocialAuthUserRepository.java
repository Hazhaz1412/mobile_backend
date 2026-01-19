package com.react.mobile.Repository;

import com.react.mobile.Entity.SocialAuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SocialAuthUserRepository extends JpaRepository<SocialAuthUser, String> { 
    Optional<SocialAuthUser> findByProviderAndProviderUserId(String provider, String providerUserId);
}