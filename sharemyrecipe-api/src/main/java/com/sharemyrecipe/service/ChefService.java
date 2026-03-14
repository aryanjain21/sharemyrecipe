package com.sharemyrecipe.service;

import com.sharemyrecipe.domain.User;
import com.sharemyrecipe.dto.UserResponse;
import com.sharemyrecipe.exception.NotFoundException;
import com.sharemyrecipe.repository.FollowRepository;
import com.sharemyrecipe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChefService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public UserResponse getChefProfile(UUID chefId) {
        User user = userRepository.findById(chefId)
                .orElseThrow(() -> new NotFoundException("Chef not found: " + chefId));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getChefProfileByHandle(String handle) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new NotFoundException("Chef not found: " + handle));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public ChefStatsResponse getStats(UUID chefId) {
        long following  = followRepository.countByFollowerId(chefId);
        long followers  = followRepository.countByFollowingId(chefId);
        return new ChefStatsResponse(chefId, following, followers);
    }

    public record ChefStatsResponse(UUID chefId, long following, long followers) {}
}
