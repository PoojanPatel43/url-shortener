package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private Long totalClicks;
    private Long clicksLast24Hours;
    private Long clicksLast7Days;
    private Long clicksLast30Days;
    private List<DailyClicks> dailyClicks;
    private List<StatEntry> topCountries;
    private List<StatEntry> topBrowsers;
    private List<StatEntry> topDevices;
    private List<StatEntry> topOperatingSystems;
    private List<StatEntry> topReferers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyClicks {
        private String date;
        private Long clicks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatEntry {
        private String name;
        private Long count;
        private Double percentage;
    }
}
