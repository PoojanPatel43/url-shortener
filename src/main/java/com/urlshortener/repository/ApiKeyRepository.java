package com.urlshortener.repository;

import com.urlshortener.entity.ApiKey;
import com.urlshortener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByUserOrderByCreatedAtDesc(User user);

    List<ApiKey> findByUserAndEnabledTrue(User user);

    boolean existsByKeyHash(String keyHash);

    @Modifying
    @Query("UPDATE ApiKey a SET a.lastUsedAt = :now WHERE a.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("now") LocalDateTime now);

    long countByUser(User user);
}
