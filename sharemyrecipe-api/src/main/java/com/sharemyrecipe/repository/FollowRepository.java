package com.sharemyrecipe.repository;

import com.sharemyrecipe.domain.Follow;
import com.sharemyrecipe.domain.FollowId;
import com.sharemyrecipe.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    @Modifying
    @Transactional
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    long countByFollowerId(UUID followerId);

    long countByFollowingId(UUID followingId);

    @Query("SELECT f.following FROM Follow f WHERE f.follower.id = :followerId")
    List<User> findFollowingByFollowerId(@Param("followerId") UUID followerId);

    @Query("SELECT f.follower FROM Follow f WHERE f.following.id = :followingId")
    List<User> findFollowersByFollowingId(@Param("followingId") UUID followingId);
}
