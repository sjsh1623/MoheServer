package com.mohe.spring.repository;

import com.mohe.spring.entity.PlaceDescriptionEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceDescriptionEmbeddingRepository extends JpaRepository<PlaceDescriptionEmbedding, Long> {

    Optional<PlaceDescriptionEmbedding> findByPlaceId(Long placeId);

    boolean existsByPlaceId(Long placeId);

    @Modifying
    @Query("DELETE FROM PlaceDescriptionEmbedding e WHERE e.placeId = :placeId")
    void deleteByPlaceId(@Param("placeId") Long placeId);

    /**
     * 벡터 유사도 검색 (cosine distance) + 거리 필터
     * 사용자 프롬프트 임베딩과 가장 유사한 장소 반환
     */
    @Query(value = """
        SELECT p.id, p.name, p.latitude, p.longitude, p.road_address, p.rating,
               1 - (pde.embedding <=> CAST(:queryEmbedding AS vector)) as similarity
        FROM places p
        JOIN place_description_embeddings pde ON pde.place_id = p.id
        WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL
        AND (
            6371 * acos(
                LEAST(1.0, GREATEST(-1.0,
                    cos(radians(:latitude)) * cos(radians(CAST(p.latitude AS DOUBLE PRECISION))) *
                    cos(radians(CAST(p.longitude AS DOUBLE PRECISION)) - radians(:longitude)) +
                    sin(radians(:latitude)) * sin(radians(CAST(p.latitude AS DOUBLE PRECISION)))
                ))
            )
        ) <= :distance
        ORDER BY pde.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :lim
    """, nativeQuery = true)
    List<Object[]> findSimilarPlacesByPrompt(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("distance") Double distance,
        @Param("lim") int limit
    );

    @Query(value = """
        SELECT p.id, p.name, p.latitude, p.longitude, p.road_address, p.rating,
               1 - (pde.embedding <=> CAST(:queryEmbedding AS vector)) as similarity
        FROM places p
        JOIN place_description_embeddings pde ON pde.place_id = p.id
        ORDER BY pde.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :lim
    """, nativeQuery = true)
    List<Object[]> findSimilarPlaces(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("lim") int limit
    );
}
