package com.mohe.spring.service;

import com.mohe.spring.entity.VisitorLog;
import com.mohe.spring.repository.VisitorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final VisitorLogRepository visitorLogRepository;

    /**
     * Record a pageview with parsed User-Agent info
     */
    public void recordPageview(String sessionId, String ipAddress, String userAgent,
                                String pagePath, String referrer, Long userId) {
        VisitorLog visitorLog = new VisitorLog();
        visitorLog.setSessionId(sessionId);
        visitorLog.setIpAddress(ipAddress);
        visitorLog.setUserAgent(userAgent);
        visitorLog.setPagePath(pagePath);
        visitorLog.setReferrer(referrer);
        visitorLog.setUserId(userId);

        // Parse User-Agent
        Map<String, String> parsed = parseUserAgent(userAgent);
        visitorLog.setDeviceType(parsed.get("deviceType"));
        visitorLog.setOs(parsed.get("os"));
        visitorLog.setBrowser(parsed.get("browser"));

        visitorLogRepository.save(visitorLog);
    }

    /**
     * Get summary stats: today, this week, this month (pageviews + unique visitors)
     */
    public Map<String, Object> getSummary() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime startOfWeek = startOfDay.minusDays(now.getDayOfWeek().getValue() - 1);
        OffsetDateTime startOfMonth = startOfDay.withDayOfMonth(1);

        Map<String, Object> summary = new LinkedHashMap<>();

        Map<String, Object> today = new LinkedHashMap<>();
        today.put("pageviews", visitorLogRepository.countByCreatedAtAfter(startOfDay));
        today.put("uniqueVisitors", visitorLogRepository.countDistinctSessionIdByCreatedAtAfter(startOfDay));
        summary.put("today", today);

        Map<String, Object> thisWeek = new LinkedHashMap<>();
        thisWeek.put("pageviews", visitorLogRepository.countByCreatedAtAfter(startOfWeek));
        thisWeek.put("uniqueVisitors", visitorLogRepository.countDistinctSessionIdByCreatedAtAfter(startOfWeek));
        summary.put("thisWeek", thisWeek);

        Map<String, Object> thisMonth = new LinkedHashMap<>();
        thisMonth.put("pageviews", visitorLogRepository.countByCreatedAtAfter(startOfMonth));
        thisMonth.put("uniqueVisitors", visitorLogRepository.countDistinctSessionIdByCreatedAtAfter(startOfMonth));
        summary.put("thisMonth", thisMonth);

        return summary;
    }

    /**
     * Get hourly stats for the last 24 hours
     */
    public List<Map<String, Object>> getHourlyStats() {
        OffsetDateTime since = OffsetDateTime.now().minusHours(24);
        List<Object[]> rows = visitorLogRepository.findHourlyStats(since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("hour", ((Number) row[0]).intValue());
            entry.put("count", ((Number) row[1]).longValue());
            result.add(entry);
        }
        return result;
    }

    /**
     * Get device type breakdown
     */
    public List<Map<String, Object>> getDeviceStats() {
        OffsetDateTime since = OffsetDateTime.now().minusDays(30);
        List<Object[]> rows = visitorLogRepository.findDeviceStats(since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("deviceType", row[0] != null ? row[0].toString() : "unknown");
            entry.put("count", ((Number) row[1]).longValue());
            result.add(entry);
        }
        return result;
    }

    /**
     * Get top visited pages
     */
    public List<Map<String, Object>> getTopPages(int limit) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(30);
        List<Object[]> rows = visitorLogRepository.findTopPages(since, PageRequest.of(0, limit));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("pagePath", row[0] != null ? row[0].toString() : "unknown");
            entry.put("count", ((Number) row[1]).longValue());
            result.add(entry);
        }
        return result;
    }

    /**
     * Get recent visitors with pagination
     */
    public Map<String, Object> getRecentVisitors(int page, int size) {
        Page<VisitorLog> visitors = visitorLogRepository.findRecentVisitors(PageRequest.of(page, size));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", visitors.getContent());
        result.put("totalElements", visitors.getTotalElements());
        result.put("totalPages", visitors.getTotalPages());
        result.put("page", visitors.getNumber());
        result.put("size", visitors.getSize());
        return result;
    }

    /**
     * Get browser breakdown
     */
    public List<Map<String, Object>> getBrowserStats() {
        OffsetDateTime since = OffsetDateTime.now().minusDays(30);
        List<Object[]> rows = visitorLogRepository.findBrowserStats(since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("browser", row[0] != null ? row[0].toString() : "unknown");
            entry.put("count", ((Number) row[1]).longValue());
            result.add(entry);
        }
        return result;
    }

    /**
     * Get OS breakdown
     */
    public List<Map<String, Object>> getOsStats() {
        OffsetDateTime since = OffsetDateTime.now().minusDays(30);
        List<Object[]> rows = visitorLogRepository.findOsStats(since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("os", row[0] != null ? row[0].toString() : "unknown");
            entry.put("count", ((Number) row[1]).longValue());
            result.add(entry);
        }
        return result;
    }

    /**
     * Simple User-Agent parser - extracts device type, OS, and browser.
     * No external library needed.
     */
    Map<String, String> parseUserAgent(String ua) {
        Map<String, String> result = new HashMap<>();

        if (ua == null || ua.isBlank()) {
            result.put("deviceType", "unknown");
            result.put("os", "unknown");
            result.put("browser", "unknown");
            return result;
        }

        String uaLower = ua.toLowerCase();

        // Device type
        if (uaLower.contains("mobile") || uaLower.contains("android") || uaLower.contains("iphone")) {
            result.put("deviceType", "mobile");
        } else if (uaLower.contains("tablet") || uaLower.contains("ipad")) {
            result.put("deviceType", "tablet");
        } else {
            result.put("deviceType", "desktop");
        }

        // OS
        if (uaLower.contains("iphone") || uaLower.contains("ipad") || uaLower.contains("ios")) {
            result.put("os", "iOS");
        } else if (uaLower.contains("android")) {
            result.put("os", "Android");
        } else if (uaLower.contains("windows")) {
            result.put("os", "Windows");
        } else if (uaLower.contains("macintosh") || uaLower.contains("mac os")) {
            result.put("os", "Mac");
        } else if (uaLower.contains("linux")) {
            result.put("os", "Linux");
        } else {
            result.put("os", "Other");
        }

        // Browser
        if (uaLower.contains("edg/") || uaLower.contains("edge/")) {
            result.put("browser", "Edge");
        } else if (uaLower.contains("opr/") || uaLower.contains("opera")) {
            result.put("browser", "Opera");
        } else if (uaLower.contains("samsungbrowser")) {
            result.put("browser", "Samsung Browser");
        } else if (uaLower.contains("chrome") && !uaLower.contains("chromium")) {
            result.put("browser", "Chrome");
        } else if (uaLower.contains("safari") && !uaLower.contains("chrome")) {
            result.put("browser", "Safari");
        } else if (uaLower.contains("firefox")) {
            result.put("browser", "Firefox");
        } else {
            result.put("browser", "Other");
        }

        return result;
    }
}
