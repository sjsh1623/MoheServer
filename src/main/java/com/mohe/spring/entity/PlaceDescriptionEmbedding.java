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
 * mohe_description 문장 전체의 임베딩.
 * 프롬프트 기반 벡터 유사도 검색에 사용.
 * 장소당 1개 (키워드 임베딩은 9개).
 */
@Entity
@Table(name = "place_description_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDescriptionEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false, unique = true)
    private Long placeId;

    @Column(name = "description_text", nullable = false, columnDefinition = "TEXT")
    private String descriptionText;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    @Type(PGvectorType.class)
    private PGvector embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public PlaceDescriptionEmbedding(Long placeId, String descriptionText, float[] embeddingArray) {
        this.placeId = placeId;
        this.descriptionText = descriptionText;
        this.embedding = new PGvector(embeddingArray);
        this.createdAt = LocalDateTime.now();
    }

    public float[] getEmbeddingAsArray() {
        return embedding != null ? embedding.toArray() : null;
    }
}
