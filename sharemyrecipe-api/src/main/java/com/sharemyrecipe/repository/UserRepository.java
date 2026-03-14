package com.sharemyrecipe.repository;

import com.sharemyrecipe.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByHandle(String handle);

    boolean existsByEmail(String email);

    boolean existsByHandle(String handle);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithDetails(@Param("id") UUID id);
}
