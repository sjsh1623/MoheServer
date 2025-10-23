package com.mohe.spring.repository;

import com.mohe.spring.entity.PlaceKeywordEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing place keyword embeddings
 */
@Repository
public interface PlaceKeywordEmbeddingRepository extends JpaRepository<PlaceKeywordEmbedding, Long> {

    /**
     * Find all embeddings for a specific place
     */
    List<PlaceKeywordEmbedding> findByPlaceId(Long placeId);

    /**
     * Check if embeddings exist for a specific place
     */
    boolean existsByPlaceId(Long placeId);

    /**
     * Count embeddings for a specific place
     */
    long countByPlaceId(Long placeId);

    /**
     * Delete all embeddings for a specific place
     */
    @Modifying
    @Query("DELETE FROM PlaceKeywordEmbedding pke WHERE pke.placeId = :placeId")
    void deleteByPlaceId(@Param("placeId") Long placeId);

    /**
     * Find embeddings by keyword (exact match)
     */
    List<PlaceKeywordEmbedding> findByKeyword(String keyword);

    /**
     * Find similar keywords using vector similarity search
     * Uses cosine similarity to find the most similar embeddings
     */
    @Query(value = """
        SELECT *
        FROM place_keyword_embeddings
        WHERE embedding IS NOT NULL
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<PlaceKeywordEmbedding> findSimilarByEmbedding(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("limit") int limit
    );

    /**
     * Find all place IDs that have embeddings
     */
    @Query("SELECT DISTINCT pke.placeId FROM PlaceKeywordEmbedding pke")
    List<Long> findDistinctPlaceIds();
}
