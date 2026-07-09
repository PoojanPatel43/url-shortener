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
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "URL click analytics endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{shortCode:[A-Za-z0-9]+}")
    @Operation(summary = "Get analytics for a URL", description = "Returns detailed click analytics for a shortened URL")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @Parameter(description = "The short code of the URL") @PathVariable String shortCode,
            @Parameter(description = "Number of days to include in daily click stats (1-365, default: 30)")
            @RequestParam(defaultValue = "30") @Positive @Max(365) int days,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("User {} fetching analytics for shortCode: {} (days: {})", userDetails.getUsername(), shortCode, days);
        AnalyticsResponse analytics = analyticsService.getAnalytics(shortCode, userDetails.toUser(), days);
        log.debug("Analytics retrieved - Total clicks: {}, Unique visitors: {}",
                analytics.getTotalClicks(), analytics.getUniqueVisitors());
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
