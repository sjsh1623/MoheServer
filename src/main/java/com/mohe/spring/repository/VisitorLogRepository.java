package com.mohe.spring.repository;

import com.mohe.spring.entity.VisitorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {

    long countByCreatedAtAfter(OffsetDateTime after);

    @Query("SELECT COUNT(DISTINCT v.sessionId) FROM VisitorLog v WHERE v.createdAt > :after")
    long countDistinctSessionIdByCreatedAtAfter(@Param("after") OffsetDateTime after);

    long countByDeviceTypeAndCreatedAtAfter(String deviceType, OffsetDateTime after);

    @Query(value = "SELECT v.page_path, COUNT(*) as cnt FROM visitor_logs v WHERE v.created_at > :after GROUP BY v.page_path ORDER BY cnt DESC",
           nativeQuery = true)
    List<Object[]> findTopPages(@Param("after") OffsetDateTime after, Pageable pageable);

    @Query(value = "SELECT EXTRACT(HOUR FROM v.created_at) as hour, COUNT(*) as cnt FROM visitor_logs v WHERE v.created_at > :after GROUP BY hour ORDER BY hour",
           nativeQuery = true)
    List<Object[]> findHourlyStats(@Param("after") OffsetDateTime after);

    @Query("SELECT v FROM VisitorLog v ORDER BY v.createdAt DESC")
    Page<VisitorLog> findRecentVisitors(Pageable pageable);

    @Query(value = "SELECT v.device_type, COUNT(*) as cnt FROM visitor_logs v WHERE v.created_at > :after GROUP BY v.device_type ORDER BY cnt DESC",
           nativeQuery = true)
    List<Object[]> findDeviceStats(@Param("after") OffsetDateTime after);

    @Query(value = "SELECT v.browser, COUNT(*) as cnt FROM visitor_logs v WHERE v.created_at > :after GROUP BY v.browser ORDER BY cnt DESC",
           nativeQuery = true)
    List<Object[]> findBrowserStats(@Param("after") OffsetDateTime after);

    @Query(value = "SELECT v.os, COUNT(*) as cnt FROM visitor_logs v WHERE v.created_at > :after GROUP BY v.os ORDER BY cnt DESC",
           nativeQuery = true)
    List<Object[]> findOsStats(@Param("after") OffsetDateTime after);
}
