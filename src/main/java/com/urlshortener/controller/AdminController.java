package com.urlshortener.controller;

import com.urlshortener.dto.AdminStatsResponse;
import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.UserResponse;
import com.urlshortener.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative endpoints for managing the platform")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(summary = "Get platform statistics", description = "Returns overall platform statistics including users, URLs, and clicks")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        log.info("Fetching platform statistics");
        AdminStatsResponse stats = adminService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Returns a paginated list of all users with their statistics")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<UserResponse> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details", description = "Returns detailed information about a specific user")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "The ID of the user") @PathVariable Long userId) {

        UserResponse user = adminService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/users/{userId}/toggle-status")
    @Operation(summary = "Toggle user status", description = "Enables or disables a user account")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
            @Parameter(description = "The ID of the user") @PathVariable Long userId) {

        adminService.toggleUserStatus(userId);
        return ResponseEntity.ok(ApiResponse.success("User status toggled successfully", null));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete a user", description = "Permanently deletes a user and all their associated data")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "The ID of the user") @PathVariable Long userId) {

        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}
