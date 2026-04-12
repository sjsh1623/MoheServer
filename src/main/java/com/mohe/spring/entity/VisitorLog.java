package com.mohe.spring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "visitor_logs", indexes = {
    @Index(name = "idx_visitor_logs_created_at", columnList = "created_at"),
    @Index(name = "idx_visitor_logs_session_id", columnList = "session_id"),
    @Index(name = "idx_visitor_logs_page_path", columnList = "page_path")
})
@Getter
@Setter
public class VisitorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "os")
    private String os;

    @Column(name = "browser")
    private String browser;

    @Column(name = "page_path")
    private String pagePath;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
