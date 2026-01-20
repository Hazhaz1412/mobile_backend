package com.react.mobile.Config;

import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Tạo admin user mặc định nếu chưa tồn tại
        try {
            if (!authUserRepository.findByEmail("admin@test.com").isPresent() 
                && !authUserRepository.findByUsername("admin").isPresent()) {
                AuthUser adminUser = AuthUser.builder()
                        .username("admin")
                        .email("admin@test.com")
                        .password(passwordEncoder.encode("admin123"))
                        .isActive(true)
                        .isSuperuser(true)
                        .isStaff(true)
                        .build();
                
                authUserRepository.save(adminUser);
                log.info("========================================");
                log.info("Created default admin user:");
                log.info("Email: admin@test.com");
                log.info("Password: admin123");
                log.info("========================================");
            } else {
                log.info("Admin user already exists, skipping creation");
            }
        } catch (Exception e) {
            log.warn("Admin user already exists in database");
        }
        
        // Tạo test user thường nếu chưa tồn tại
        try {
            if (!authUserRepository.findByEmail("user@test.com").isPresent() 
                && !authUserRepository.findByUsername("testuser").isPresent()) {
                AuthUser testUser = AuthUser.builder()
                        .username("testuser")
                        .email("user@test.com")
                        .password(passwordEncoder.encode("user123"))
                        .isActive(true)
                        .isSuperuser(false)
                        .isStaff(false)
                        .build();
                
                authUserRepository.save(testUser);
                log.info("========================================");
                log.info("Created default test user:");
                log.info("Email: user@test.com");
                log.info("Password: user123");
                log.info("========================================");
            } else {
                log.info("Test user already exists, skipping creation");
            }
        } catch (Exception e) {
            log.warn("Test user already exists in database");
        }
    }
}
