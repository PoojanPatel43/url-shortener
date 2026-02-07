package com.urlshortener.controller;

import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.UpdateUserRequest;
import com.urlshortener.dto.UserResponse;
import com.urlshortener.security.CustomUserDetails;
import com.urlshortener.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User profile management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the profile of the authenticated user with statistics")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UserResponse response = userService.getCurrentUser(userDetails.toUser());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UserResponse response = userService.updateUser(userDetails.toUser(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user account", description = "Permanently deletes the authenticated user's account and all associated data")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        userService.deleteUser(userDetails.toUser());
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }
}
