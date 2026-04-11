package com.urlshortener.service;

import com.urlshortener.dto.AdminStatsResponse;
import com.urlshortener.dto.UserResponse;
import com.urlshortener.entity.User;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final ClickAnalyticsRepository clickAnalyticsRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        log.debug("Fetching platform statistics");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);

        long totalUsers = userRepository.count();
        long totalUrls = urlRepository.count();
        long activeUrls = urlRepository.countActiveUrls();
        long totalClicks = clickAnalyticsRepository.count();
        long clicksToday = clickAnalyticsRepository.countClicksSinceDate(startOfDay);
        long clicksThisWeek = clickAnalyticsRepository.countClicksSinceDate(startOfWeek);
        long clicksThisMonth = clickAnalyticsRepository.countClicksSinceDate(startOfMonth);

        log.info("Platform stats - Users: {}, URLs: {}, Active URLs: {}, Total clicks: {}",
                totalUsers, totalUrls, activeUrls, totalClicks);

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalUrls(totalUrls)
                .activeUrls(activeUrls)
                .totalClicks(totalClicks)
                .clicksToday(clicksToday)
                .clicksThisWeek(clicksThisWeek)
                .clicksThisMonth(clicksThisMonth)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> {
                    Long totalUrls = urlRepository.countByUser(user);
                    Long totalClicks = urlRepository.getTotalClicksByUser(user);

                    return UserResponse.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .name(user.getName())
                            .role(user.getRole().name())
                            .totalUrls(totalUrls)
                            .totalClicks(totalClicks != null ? totalClicks : 0L)
                            .createdAt(user.getCreatedAt())
                            .build();
                });
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Long totalUrls = urlRepository.countByUser(user);
        Long totalClicks = urlRepository.getTotalClicksByUser(user);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .totalUrls(totalUrls)
                .totalClicks(totalClicks != null ? totalClicks : 0L)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean previousStatus = user.getEnabled();
        user.setEnabled(!previousStatus);
        userRepository.save(user);
        log.info("User {} status changed: {} -> {}", user.getEmail(), previousStatus, user.getEnabled());
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        long userUrls = urlRepository.countByUser(user);
        userRepository.delete(user);
        log.warn("Admin deleted user: {} (had {} URLs)", user.getEmail(), userUrls);
    }
}
