package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;  

@Entity
@Table(name = "auth_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser implements UserDetails {

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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive != null && isActive;
    }
}