package com.react.mobile.Repository;

import com.react.mobile.Entity.PlaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceEmbeddingRepository extends JpaRepository<PlaceEmbedding, Long> {

    Optional<PlaceEmbedding> findByPlaceId(String placeId);

    List<PlaceEmbedding> findByCategory(String category);

    List<PlaceEmbedding> findAllByPlaceIdIn(List<String> placeIds);
}
