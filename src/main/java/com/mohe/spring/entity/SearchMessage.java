package com.mohe.spring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "search_messages")
@Getter @Setter @NoArgsConstructor
public class SearchMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private SearchConversation conversation;

    @Column(name = "role", nullable = false, length = 20)
    private String role; // "user" or "assistant"

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "place_ids", columnDefinition = "BIGINT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Long[] placeIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public SearchMessage(SearchConversation conversation, String role, String content, Long[] placeIds) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.placeIds = placeIds;
    }
}
