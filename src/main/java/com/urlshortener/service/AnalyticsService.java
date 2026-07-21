package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.entity.ClickAnalytics;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.exception.BadRequestException;
import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.UrlRepository;
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
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
        try {
            Url url = urlService.getUrlByShortCode(shortCode);

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

            log.debug("Recorded click for URL: {} from {} via {}", shortCode, ipAddress, parseBrowser(userAgent));
        } catch (Exception e) {
            log.error("Failed to record click analytics for URL: {}", shortCode, e);
        }
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode, User user, int days) {
        log.debug("Fetching analytics for shortCode: {} by user: {} (days: {})", shortCode, user.getEmail(), days);
        Url url = urlService.getUrlByShortCode(shortCode);
        validateOwnership(url, user);

        LocalDateTime now = LocalDateTime.now();
        long totalClicks = url.getClickCount();
        long uniqueVisitors = clickAnalyticsRepository.countUniqueVisitorsByUrl(url);
        long clicks24h = clickAnalyticsRepository.countClicksSince(url, now.minusHours(24));
        long clicks7d = clickAnalyticsRepository.countClicksSince(url, now.minusDays(7));
        long clicks30d = clickAnalyticsRepository.countClicksSince(url, now.minusDays(30));

        List<AnalyticsResponse.DailyClicks> dailyClicks = getDailyClicks(url, days);
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

        log.debug("Analytics for {} - clicks: {}, unique: {}, 24h: {}, 7d: {}, 30d: {}",
                shortCode, totalClicks, uniqueVisitors, clicks24h, clicks7d, clicks30d);

        return AnalyticsResponse.builder()
                .totalClicks(totalClicks)
                .uniqueVisitors(uniqueVisitors)
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

    private String parseDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("mobile") || (userAgent.contains("android") && !userAgent.contains("tablet"))) {
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
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        } else if (userAgent.contains("chrome") && !userAgent.contains("edg")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            return "Safari";
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
