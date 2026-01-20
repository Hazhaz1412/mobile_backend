package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.react.mobile.Entity.Enums.InterestType;  
import com.react.mobile.Entity.Enums.InterestType;
import java.util.Set; 
import java.time.LocalDateTime;
import java.util.UUID;  

@Entity
@Table(name = "user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AuthUser authUser;

    @Column(name = "first_name", length = 50)
    private String firstName;
    @Column(name = "last_name", length = 50)
    private String lastName;
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;
    @Column(name ="gender", length = 20)
    private String gender;
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    @Column(name = "address", length = 255)
    private String address;
    @Column(name="travel_style", length = 100)
    private String travelStyle;
    @ElementCollection(targetClass = InterestType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING) 
    @Column(name = "interest")
    private Set<InterestType> interests;
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;
    @Column(name = "profile_picture_url", length = 255)
    private String profilePictureUrl;

}
