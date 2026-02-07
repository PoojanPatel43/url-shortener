package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.entity.ClickAnalytics;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.exception.BadRequestException;
import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final UrlRepository urlRepository;
    private final UrlService urlService;

    @Async
    @Transactional
    public void recordClick(Url url, HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            String referer = request.getHeader("Referer");
            String ipAddress = getClientIpAddress(request);

            ClickAnalytics analytics = ClickAnalytics.builder()
                    .url(url)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referer(referer)
                    .deviceType(parseDeviceType(userAgent))
                    .browser(parseBrowser(userAgent))
                    .os(parseOperatingSystem(userAgent))
                    .build();

            clickAnalyticsRepository.save(analytics);
            urlService.incrementClickCount(url.getId());

            log.debug("Recorded click for URL: {}", url.getShortCode());
        } catch (Exception e) {
            log.error("Failed to record click analytics for URL: {}", url.getShortCode(), e);
        }
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode, User user) {
        Url url = urlService.getUrlByShortCode(shortCode);
        validateOwnership(url, user);

        LocalDateTime now = LocalDateTime.now();
        long totalClicks = url.getClickCount();
        long clicks24h = clickAnalyticsRepository.countClicksSince(url, now.minusHours(24));
        long clicks7d = clickAnalyticsRepository.countClicksSince(url, now.minusDays(7));
        long clicks30d = clickAnalyticsRepository.countClicksSince(url, now.minusDays(30));

        List<AnalyticsResponse.DailyClicks> dailyClicks = getDailyClicks(url, 30);
        List<AnalyticsResponse.StatEntry> topCountries = convertToStatEntries(
                clickAnalyticsRepository.getCountryStats(url), totalClicks);
        List<AnalyticsResponse.StatEntry> topBrowsers = convertToStatEntries(
                clickAnalyticsRepository.getBrowserStats(url), totalClicks);
        List<AnalyticsResponse.StatEntry> topDevices = convertToStatEntries(
                clickAnalyticsRepository.getDeviceStats(url), totalClicks);
        List<AnalyticsResponse.StatEntry> topOs = convertToStatEntries(
                clickAnalyticsRepository.getOsStats(url), totalClicks);
        List<AnalyticsResponse.StatEntry> topReferers = convertToStatEntries(
                clickAnalyticsRepository.getRefererStats(url), totalClicks);

        return AnalyticsResponse.builder()
                .totalClicks(totalClicks)
                .clicksLast24Hours(clicks24h)
                .clicksLast7Days(clicks7d)
                .clicksLast30Days(clicks30d)
                .dailyClicks(dailyClicks)
                .topCountries(topCountries)
                .topBrowsers(topBrowsers)
                .topDevices(topDevices)
                .topOperatingSystems(topOs)
                .topReferers(topReferers)
                .build();
    }

    private List<AnalyticsResponse.DailyClicks> getDailyClicks(Url url, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = clickAnalyticsRepository.getDailyClickStats(url, startDate);

        return results.stream()
                .map(row -> AnalyticsResponse.DailyClicks.builder()
                        .date(row[0].toString())
                        .clicks((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    private List<AnalyticsResponse.StatEntry> convertToStatEntries(List<Object[]> stats, long totalClicks) {
        return stats.stream()
                .limit(10)
                .map(row -> {
                    String name = row[0] != null ? row[0].toString() : "Unknown";
                    Long count = (Long) row[1];
                    double percentage = totalClicks > 0 ? (count * 100.0) / totalClicks : 0;
                    return AnalyticsResponse.StatEntry.builder()
                            .name(name)
                            .count(count)
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_CLIENT_IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String parseDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("mobile") || userAgent.contains("android") && userAgent.contains("mobile")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }

    private String parseBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("edg")) {
            return "Edge";
        } else if (userAgent.contains("chrome") && !userAgent.contains("edg")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            return "Safari";
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        } else {
            return "Other";
        }
    }

    private String parseOperatingSystem(String userAgent) {
        if (userAgent == null) return "Unknown";
        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac os") || userAgent.contains("macintosh")) {
            return "macOS";
        } else if (userAgent.contains("linux") && !userAgent.contains("android")) {
            return "Linux";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone") || userAgent.contains("ipad") || userAgent.contains("ios")) {
            return "iOS";
        } else {
            return "Other";
        }
    }

    private void validateOwnership(Url url, User user) {
        if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to access analytics for this URL");
        }
    }
}
