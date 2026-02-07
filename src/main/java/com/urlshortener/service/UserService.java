package com.urlshortener.service;

import com.urlshortener.dto.UpdateUserRequest;
import com.urlshortener.dto.UserResponse;
import com.urlshortener.entity.User;
import com.urlshortener.exception.BadRequestException;
import com.urlshortener.exception.ResourceNotFoundException;
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

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(User user) {
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long totalUrls = urlRepository.countByUser(fullUser);
        Long totalClicks = urlRepository.getTotalClicksByUser(fullUser);

        return UserResponse.builder()
                .id(fullUser.getId())
                .email(fullUser.getEmail())
                .name(fullUser.getName())
                .role(fullUser.getRole().name())
                .totalUrls(totalUrls)
                .totalClicks(totalClicks != null ? totalClicks : 0L)
                .createdAt(fullUser.getCreatedAt())
                .build();
    }

    @Transactional
    public UserResponse updateUser(User user, UpdateUserRequest request) {
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            fullUser.setName(request.getName());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new BadRequestException("Current password is required to change password");
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), fullUser.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }

            fullUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
            log.info("Password updated for user: {}", fullUser.getEmail());
        }

        userRepository.save(fullUser);
        log.info("User profile updated: {}", fullUser.getEmail());

        return getCurrentUser(fullUser);
    }

    @Transactional
    public void deleteUser(User user) {
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(fullUser);
        log.info("User deleted: {}", fullUser.getEmail());
    }
}
