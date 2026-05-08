package com.urlshortener.controller;

import com.urlshortener.entity.Url;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
@Tag(name = "Redirect", description = "URL redirection endpoint")
public class RedirectController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to original URL", description = "Redirects to the original URL associated with the short code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "URL not found or expired")
    })
    public ResponseEntity<Void> redirect(
            @Parameter(description = "The short code of the URL") @PathVariable String shortCode,
            HttpServletRequest request) {

        String clientIp = getClientIpAddress(request);
        log.info("Redirect request for short code: {} from IP: {}", shortCode, clientIp);

        // Get the URL entity for analytics
        Url url = urlService.getUrlByShortCode(shortCode);

        // Get the original URL (this validates active status and expiration)
        String originalUrl = urlService.getOriginalUrl(shortCode);

        // Record analytics asynchronously
        analyticsService.recordClick(url, request);

        String userAgent = request.getHeader("User-Agent");
        log.debug("Redirecting {} to: {} (User-Agent: {})", shortCode, originalUrl,
                userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 50)) : "unknown");

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
