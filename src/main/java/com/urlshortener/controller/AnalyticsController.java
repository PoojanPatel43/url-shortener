package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.ApiResponse;
import com.urlshortener.security.CustomUserDetails;
import com.urlshortener.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "URL click analytics endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Get analytics for a URL", description = "Returns detailed click analytics for a shortened URL")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @Parameter(description = "The short code of the URL") @PathVariable String shortCode,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AnalyticsResponse analytics = analyticsService.getAnalytics(shortCode, userDetails.toUser());
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
