package com.react.mobile.Repository;

import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.SocialAuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SocialAuthUserRepository extends JpaRepository<SocialAuthUser, String> { 
    Optional<SocialAuthUser> findByProviderAndProviderUserId(String provider, String providerUserId);
    
    /**
     * Tìm social auth bằng provider và user
     */
    Optional<SocialAuthUser> findByProviderAndAuthUser(String provider, AuthUser authUser);
    
    /**
     * Kiểm tra xem user có liên kết với provider không
     */
    boolean existsByProviderAndAuthUser(String provider, AuthUser authUser);
    
    /**
     * Xóa liên kết social auth
     */
    void deleteByProviderAndAuthUser(String provider, AuthUser authUser);

    void deleteByAuthUser(AuthUser authUser);
}
