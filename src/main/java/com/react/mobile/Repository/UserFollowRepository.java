package com.react.mobile.Repository;

import com.react.mobile.Entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Query("SELECT uf FROM UserFollow uf JOIN FETCH uf.following WHERE uf.follower.id = :userId ORDER BY uf.createdAt DESC")
    Page<UserFollow> findFollowingByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT uf FROM UserFollow uf JOIN FETCH uf.follower WHERE uf.following.id = :userId ORDER BY uf.createdAt DESC")
    Page<UserFollow> findFollowersByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    @Query("SELECT uf.following.id FROM UserFollow uf WHERE uf.follower.id = :userId")
    List<Long> findFollowingIdsByUserId(@Param("userId") Long userId);
}
