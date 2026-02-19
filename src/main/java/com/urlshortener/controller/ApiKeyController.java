package com.urlshortener.controller;

import com.urlshortener.dto.ApiKeyRequest;
import com.urlshortener.dto.ApiKeyResponse;
import com.urlshortener.dto.ApiResponse;
import com.urlshortener.security.CustomUserDetails;
import com.urlshortener.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "API key management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @Operation(summary = "Create a new API key", description = "Creates a new API key for programmatic access. The key is only shown once upon creation.")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(
            @Valid @RequestBody ApiKeyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Creating API key: {}", request.getName());
        ApiKeyResponse response = apiKeyService.createApiKey(request, userDetails.toUser());
        log.info("API key created successfully");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("API key created successfully. Save this key - it won't be shown again!", response));
    }

    @GetMapping
    @Operation(summary = "List all API keys", description = "Returns all API keys for the authenticated user")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> listApiKeys(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ApiKeyResponse> keys = apiKeyService.getUserApiKeys(userDetails.toUser());
        return ResponseEntity.ok(ApiResponse.success(keys));
    }

    @PatchMapping("/{keyId}/revoke")
    @Operation(summary = "Revoke an API key", description = "Disables an API key without deleting it")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(
            @Parameter(description = "The ID of the API key") @PathVariable Long keyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        apiKeyService.revokeApiKey(keyId, userDetails.toUser());
        return ResponseEntity.ok(ApiResponse.success("API key revoked successfully", null));
    }

    @DeleteMapping("/{keyId}")
    @Operation(summary = "Delete an API key", description = "Permanently deletes an API key")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(
            @Parameter(description = "The ID of the API key") @PathVariable Long keyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        apiKeyService.deleteApiKey(keyId, userDetails.toUser());
        return ResponseEntity.ok(ApiResponse.success("API key deleted successfully", null));
    }
}
