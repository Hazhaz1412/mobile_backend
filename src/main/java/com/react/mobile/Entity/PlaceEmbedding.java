package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "place_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false, unique = true, length = 255)
    private String placeId;

    @Column(name = "place_name", length = 500)
    private String placeName;

    @Column(name = "category", length = 50)
    private String category;

    /** JSON array of tags used for vector computation */
    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    /** JSON array of doubles representing the tag vector */
    @Column(name = "tag_vector", columnDefinition = "TEXT")
    private String tagVector;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
