package com.react.mobile.Repository;
import com.react.mobile.Entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
     
    VerificationToken findByToken(String token);
}