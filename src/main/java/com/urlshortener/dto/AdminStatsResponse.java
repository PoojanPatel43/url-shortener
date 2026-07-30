package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private Long totalUsers;
    private Long newUsersToday;
    private Long disabledUsers;
    private Long totalUrls;
    private Long activeUrls;
    private Long urlsCreatedToday;
    private Long totalClicks;
    private Long clicksToday;
    private Long clicksThisWeek;
    private Long clicksThisMonth;
}
