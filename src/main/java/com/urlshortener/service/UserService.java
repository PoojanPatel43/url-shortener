package com.urlshortener.service;

import com.urlshortener.dto.UpdateUserRequest;
import com.urlshortener.dto.UserResponse;
import com.urlshortener.entity.User;
import com.urlshortener.exception.BadRequestException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.RefreshTokenRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(User user) {
        log.debug("Fetching profile for user: {}", user.getId());

        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(fullUser);
    }

    @Transactional
    public UserResponse updateUser(User user, UpdateUserRequest request) {
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            fullUser.setName(request.getName().trim());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new BadRequestException("Current password is required to change password");
            }

            if (request.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
                throw new BadRequestException("New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), fullUser.getPassword())) {
                log.warn("Failed password change attempt for user: {}", fullUser.getEmail());
                throw new BadRequestException("Current password is incorrect");
            }

            if (passwordEncoder.matches(request.getNewPassword(), fullUser.getPassword())) {
                throw new BadRequestException("New password must be different from current password");
            }

            fullUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
            refreshTokenRepository.deleteByUser(fullUser);
            log.info("Password updated and refresh tokens revoked for user: {}", fullUser.getEmail());
        }

        userRepository.save(fullUser);
        log.info("User profile updated: {}", fullUser.getEmail());

        return mapToUserResponse(fullUser);
    }

    private UserResponse mapToUserResponse(User user) {
        Long totalUrls = urlRepository.countByUser(user);
        Long totalClicks = urlRepository.getTotalClicksByUser(user);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .totalUrls(totalUrls)
                .totalClicks(totalClicks != null ? totalClicks : 0L)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteUser(User user) {
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(fullUser);
        log.info("User deleted: {}", fullUser.getEmail());
    }
}
