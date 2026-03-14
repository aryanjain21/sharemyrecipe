package com.sharemyrecipe.service;

import com.sharemyrecipe.domain.*;
import com.sharemyrecipe.dto.*;
import com.sharemyrecipe.exception.*;
import com.sharemyrecipe.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public void follow(UUID followerId, UUID targetId) {
        if (followerId.equals(targetId)) {
            throw new BadRequestException("Cannot follow yourself");
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("Chef not found: " + targetId));

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, targetId)) {
            throw new ConflictException("Already following this chef");
        }

        Follow follow = Follow.builder()
                .follower(follower)
                .following(target)
                .build();
        followRepository.save(follow);
        log.info("User {} now follows {}", followerId, targetId);
    }

    @Transactional
    public void unfollow(UUID followerId, UUID targetId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, targetId)) {
            throw new NotFoundException("Not following this chef");
        }
        followRepository.deleteByFollowerIdAndFollowingId(followerId, targetId);
        log.info("User {} unfollowed {}", followerId, targetId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFollowing(UUID userId) {
        return followRepository.findFollowingByFollowerId(userId)
                .stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFollowers(UUID userId) {
        return followRepository.findFollowersByFollowingId(userId)
                .stream().map(UserResponse::from).toList();
    }
}
