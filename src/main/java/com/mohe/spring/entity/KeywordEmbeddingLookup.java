package com.mohe.spring.entity;

import com.mohe.spring.config.PGvectorType;
import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

/**
 * Global keyword → embedding lookup table.
 * Caches embeddings so identical keywords are only sent to OpenAI once.
 */
@Entity
@Table(name = "keyword_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordEmbeddingLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", nullable = false, unique = true, columnDefinition = "TEXT")
    private String keyword;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    @Type(PGvectorType.class)
    private PGvector embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public KeywordEmbeddingLookup(String keyword, float[] embeddingArray) {
        this.keyword = keyword;
        this.embedding = new PGvector(embeddingArray);
        this.createdAt = LocalDateTime.now();
    }

    public float[] getEmbeddingAsArray() {
        return embedding != null ? embedding.toArray() : null;
    }
}
