package com.urlshortener.controller;

import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.User;
import com.urlshortener.security.CustomUserDetails;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/urls")
@RequiredArgsConstructor
@Tag(name = "URL Management", description = "Endpoints for creating and managing shortened URLs")
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    @Operation(summary = "Create a shortened URL", description = "Creates a new shortened URL. Can be used anonymously or with authentication.")
    public ResponseEntity<ApiResponse<UrlResponse>> shortenUrl(
            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails != null ? userDetails.toUser() : null;
        UrlResponse response = urlService.createShortUrl(request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("URL shortened successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all URLs for current user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<UrlResponse>>> getUserUrls(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<UrlResponse> urls = urlService.getUserUrls(userDetails.toUser(), pageable);
        return ResponseEntity.ok(ApiResponse.success(urls));
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Get URL details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UrlResponse>> getUrlDetails(
            @Parameter(description = "The short code of the URL") @PathVariable String shortCode,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UrlResponse response = urlService.getUrlDetails(shortCode, userDetails.toUser());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{shortCode}")
    @Operation(summary = "Update a URL", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UrlResponse>> updateUrl(
            @Parameter(description = "The short code of the URL") @PathVariable String shortCode,
            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UrlResponse response = urlService.updateUrl(shortCode, request, userDetails.toUser());
        return ResponseEntity.ok(ApiResponse.success("URL updated successfully", response));
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Delete a URL", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteUrl(
            @Parameter(description = "The short code of the URL") @PathVariable String shortCode,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        urlService.deleteUrl(shortCode, userDetails.toUser());
        return ResponseEntity.ok(ApiResponse.success("URL deleted successfully", null));
    }
}
