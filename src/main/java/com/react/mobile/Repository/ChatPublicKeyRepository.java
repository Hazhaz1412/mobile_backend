package com.react.mobile.Repository;

import com.react.mobile.Entity.ChatPublicKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatPublicKeyRepository extends JpaRepository<ChatPublicKey, Long> {

    Optional<ChatPublicKey> findByUserId(Long userId);

    List<ChatPublicKey> findByUserIdIn(Collection<Long> userIds);
}
