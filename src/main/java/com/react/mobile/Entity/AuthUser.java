package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;  

@Entity
@Table(name = "auth_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    @Builder.Default  
    private UUID uuid = UUID.randomUUID(); 

    @Column(name = "username", nullable = false, unique = true, length = 75)
    private String username;
 
    @Column(name = "password", nullable = false, length = 255) 
    private String password;

    @Column(name = "is_superuser")
    @Builder.Default
    private Boolean isSuperuser = false;  

    @Column(name = "is_staff")
    @Builder.Default
    private Boolean isStaff = false;  

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;  

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
 
    @CreationTimestamp
    @Column(name = "date_joined", updatable = false)
    private LocalDateTime dateJoined;  

    @UpdateTimestamp
    @Column(name = "last_login")
    private LocalDateTime lastLogin;  
}