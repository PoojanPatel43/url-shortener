package com.urlshortener.repository;

import com.urlshortener.entity.ClickAnalytics;
import com.urlshortener.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    Page<ClickAnalytics> findByUrlOrderByClickedAtDesc(Url url, Pageable pageable);

    List<ClickAnalytics> findByUrlAndClickedAtBetween(Url url, LocalDateTime start, LocalDateTime end);

    @Query("SELECT c.country, COUNT(c) FROM ClickAnalytics c WHERE c.url = :url GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> getCountryStats(@Param("url") Url url);

    @Query("SELECT c.browser, COUNT(c) FROM ClickAnalytics c WHERE c.url = :url GROUP BY c.browser ORDER BY COUNT(c) DESC")
    List<Object[]> getBrowserStats(@Param("url") Url url);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickAnalytics c WHERE c.url = :url GROUP BY c.deviceType ORDER BY COUNT(c) DESC")
    List<Object[]> getDeviceStats(@Param("url") Url url);

    @Query("SELECT c.os, COUNT(c) FROM ClickAnalytics c WHERE c.url = :url GROUP BY c.os ORDER BY COUNT(c) DESC")
    List<Object[]> getOsStats(@Param("url") Url url);

    @Query("SELECT c.referer, COUNT(c) FROM ClickAnalytics c WHERE c.url = :url AND c.referer IS NOT NULL GROUP BY c.referer ORDER BY COUNT(c) DESC")
    List<Object[]> getRefererStats(@Param("url") Url url);

    @Query("SELECT CAST(c.clickedAt AS date), COUNT(c) FROM ClickAnalytics c WHERE c.url = :url AND c.clickedAt >= :startDate GROUP BY CAST(c.clickedAt AS date) ORDER BY CAST(c.clickedAt AS date)")
    List<Object[]> getDailyClickStats(@Param("url") Url url, @Param("startDate") LocalDateTime startDate);

    long countByUrl(Url url);

    @Query("SELECT COUNT(c) FROM ClickAnalytics c WHERE c.url = :url AND c.clickedAt >= :startDate")
    long countClicksSince(@Param("url") Url url, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(c) FROM ClickAnalytics c WHERE c.clickedAt >= :startDate")
    long countClicksSinceDate(@Param("startDate") LocalDateTime startDate);
}
