package com.mohe.spring.repository;

import com.mohe.spring.entity.CrawlStatus;
import com.mohe.spring.entity.EmbedStatus;
import com.mohe.spring.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    Page<Place> findByCategory(String category, Pageable pageable);

    @Query("SELECT p FROM Place p WHERE p.category = :category AND p.embedStatus = 'COMPLETED'")
    Page<Place> findByCategoryAndEmbedCompleted(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM Place p WHERE p.embedStatus = 'COMPLETED' AND (p.rating >= :minRating OR p.rating IS NULL) ORDER BY p.rating DESC, p.reviewCount DESC")
    Page<Place> findTopRatedPlaces(@Param("minRating") Double minRating, Pageable pageable);

    @Query("SELECT p FROM Place p WHERE p.embedStatus = 'COMPLETED' ORDER BY p.rating DESC, p.reviewCount DESC")
    Page<Place> findPopularPlaces(Pageable pageable);

    @Query("SELECT p FROM Place p WHERE p.embedStatus = 'COMPLETED' AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.roadAddress) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Place> searchPlaces(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Place p WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL")
    List<Place> findPlacesWithLocation();

    @Query("SELECT DISTINCT p.category FROM Place p WHERE p.category IS NOT NULL")
    List<String> findAllCategories();

    // New methods for enhanced place management
    Optional<Place> findByName(String name);

    boolean existsByRoadAddress(String roadAddress);

    @Query("""
        SELECT p FROM Place p
        WHERE p.name = :name
        AND ABS(p.latitude - :latitude) < :radius
        AND ABS(p.longitude - :longitude) < :radius
        ORDER BY (
            ABS(p.latitude - :latitude) + ABS(p.longitude - :longitude)
        ) ASC
    """)
    Optional<Place> findSimilarPlace(
        @Param("name") String name,
        @Param("latitude") BigDecimal latitude,
        @Param("longitude") BigDecimal longitude,
        @Param("radius") BigDecimal radius
    );

    // New methods for age-based filtering and dynamic fetching
    @Query("""
        SELECT COUNT(*) FROM Place p
        WHERE (p.rating >= 0.0 OR p.rating IS NULL)
        AND p.embedStatus = 'COMPLETED'
    """)
    long countRecommendablePlaces();

    @Query("""
        SELECT COUNT(*) FROM Place p
        WHERE (p.rating >= 0.0 OR p.rating IS NULL)
        AND p.embedStatus = 'COMPLETED'
        AND (:category IS NULL OR p.category = :category)
    """)
    long countRecommendablePlacesByCategory(@Param("category") String category);

    @Query("""
        SELECT p FROM Place p
        WHERE (p.rating >= 0.0 OR p.rating IS NULL)
        AND p.embedStatus = 'COMPLETED'
        ORDER BY p.rating DESC, p.name ASC
    """)
    Page<Place> findRecommendablePlaces(Pageable pageable);

    @Query("SELECT p FROM Place p WHERE p.createdAt < :oldDate AND p.rating < :ratingThreshold")
    List<Place> findOldLowRatedPlaces(
        @Param("oldDate") OffsetDateTime oldDate,
        @Param("ratingThreshold") BigDecimal ratingThreshold
    );

    @Query("SELECT p FROM Place p WHERE p.latitude IS NULL OR p.longitude IS NULL")
    List<Place> findPlacesWithoutCoordinates();

    List<Place> findByLatitudeBetweenAndLongitudeBetween(
        BigDecimal minLatitude,
        BigDecimal maxLatitude,
        BigDecimal minLongitude,
        BigDecimal maxLongitude
    );

    @Query(value = """
        SELECT p.* FROM places p
        WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL
        AND p.embed_status = 'COMPLETED'
        AND (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(CAST(p.latitude AS DOUBLE PRECISION))) *
                cos(radians(CAST(p.longitude AS DOUBLE PRECISION)) - radians(:longitude)) +
                sin(radians(:latitude)) * sin(radians(CAST(p.latitude AS DOUBLE PRECISION)))
            )
        ) <= :distance
        ORDER BY p.review_count DESC, p.rating DESC
        LIMIT 20
    """, nativeQuery = true)
    List<Place> findPopularPlaces(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("distance") Double distance
    );

    /**
     * Find places near location that match time-based preferences
     */
    @Query(value = """
        SELECT p.* FROM places p
        WHERE p.latitude IS NOT NULL
        AND p.longitude IS NOT NULL
        AND p.embed_status = 'COMPLETED'
        AND (
            ABS(CAST(p.latitude AS DOUBLE PRECISION) - :latitude) * 111000 +
            ABS(CAST(p.longitude AS DOUBLE PRECISION) - :longitude) * 111000 * COS(RADIANS(:latitude))
        ) <= :distance
        AND (
            :categories IS NULL OR
            EXISTS (
                SELECT 1 FROM unnest(ARRAY[:categories]) AS cat(category)
                WHERE LOWER(p.category) LIKE LOWER('%' || cat.category || '%')
            )
        )
        AND (p.rating >= 3.0 OR p.rating IS NULL)
        ORDER BY p.rating DESC NULLS LAST, p.review_count DESC NULLS LAST
        LIMIT :#{#pageable.pageSize}
    """, nativeQuery = true)
    List<Place> findNearbyPlacesByTimePreference(
        @Param("latitude") BigDecimal latitude,
        @Param("longitude") BigDecimal longitude,
        @Param("categories") String[] categories,
        @Param("distance") Double distance,
        Pageable pageable
    );

    /**
     * Find nearby places ordered by distance (for LLM recommendations)
     */
    @Query(value = """
        SELECT p.* FROM places p
        WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL
        AND p.embed_status = 'COMPLETED'
        AND (p.rating >= 3.0 OR p.rating IS NULL)
        AND (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(CAST(p.latitude AS DOUBLE PRECISION))) *
                cos(radians(CAST(p.longitude AS DOUBLE PRECISION)) - radians(:longitude)) +
                sin(radians(:latitude)) * sin(radians(CAST(p.latitude AS DOUBLE PRECISION)))
            )
        ) <= :distance
        ORDER BY (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(CAST(p.latitude AS DOUBLE PRECISION))) *
                cos(radians(CAST(p.longitude AS DOUBLE PRECISION)) - radians(:longitude)) +
                sin(radians(:latitude)) * sin(radians(CAST(p.latitude AS DOUBLE PRECISION)))
            )
        ) ASC
        LIMIT :limit
    """, nativeQuery = true)
    List<Place> findNearbyPlacesForLLM(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("distance") Double distance,
        @Param("limit") int limit
    );

    /**
     * Find general places for LLM recommendations (when no location provided)
     */
    @Query("""
        SELECT p FROM Place p
        WHERE (p.rating >= 3.0 OR p.rating IS NULL)
        AND p.embedStatus = 'COMPLETED'
        ORDER BY p.rating DESC, p.reviewCount DESC
    """)
    List<Place> findGeneralPlacesForLLM(Pageable pageable);

    /**
     * 이미지가 없는 장소들을 찾기 (배치 이미지 업데이트용)
     */
    @Query("""
        SELECT p FROM Place p
        WHERE p.id NOT IN (
            SELECT DISTINCT pi.place.id FROM PlaceImage pi
        )
        ORDER BY p.rating DESC NULLS LAST, p.reviewCount DESC NULLS LAST
    """)
    List<Place> findPlacesWithoutImages();

    @Query("SELECT p FROM Place p WHERE p.embedStatus = :status")
    Page<Place> findByEmbedStatus(@Param("status") EmbedStatus status, Pageable pageable);

    /**
     * Find place IDs where crawl_status = PENDING
     * Returns only IDs to avoid pagination issues with collection fetching
     * Step 1: Get IDs with pagination (efficient)
     */
    @Query("""
        SELECT p.id FROM Place p
        WHERE p.crawlStatus = 'PENDING'
        ORDER BY p.id ASC
    """)
    Page<Long> findPlaceIdsForBatchProcessing(Pageable pageable);

    /**
     * Find place IDs for batch processing with filters
     * Filters out places with:
     * - Review count less than 5
     * - Categories containing '헤어', '미용실', '마트'
     * Returns only IDs to avoid pagination issues with collection fetching
     * Step 1: Get IDs with pagination (efficient)
     */
    @Query(value = """
        SELECT p.id FROM places p
        WHERE p.crawl_status = 'PENDING'
        AND (p.review_count IS NULL OR p.review_count >= 5)
        AND NOT EXISTS (
            SELECT 1 FROM unnest(p.category) AS cat
            WHERE cat ILIKE '%헤어%'
            OR cat ILIKE '%미용실%'
            OR cat ILIKE '%마트%'
        )
        ORDER BY p.id ASC
    """, nativeQuery = true)
    Page<Long> findPlaceIdsForBatchProcessingWithFilters(Pageable pageable);

    /**
     * Find place IDs where crawlStatus = COMPLETED (for embedding)
     * Returns only IDs to avoid pagination issues with collection fetching
     * Step 1: Get IDs with pagination (efficient)
     */
    @Query("""
        SELECT p.id FROM Place p
        WHERE p.crawlStatus = 'COMPLETED'
        AND p.embedStatus = 'PENDING'
        ORDER BY p.id ASC
    """)
    Page<Long> findPlaceIdsForImageUpdate(Pageable pageable);

    /**
     * Find a single Place by ID with all collections eagerly loaded
     * Step 2: Load full entity with collections (no pagination issue)
     * Note: Split into multiple queries to avoid MultipleBagFetchException
     */
    @EntityGraph(attributePaths = {"descriptions"})
    @Query("SELECT p FROM Place p WHERE p.id = :id")
    Optional<Place> findByIdWithCollections(@Param("id") Long id);

    /**
     * Find top 5 places that are not ready for testing
     */
    @Query("""
        SELECT p FROM Place p
        WHERE p.embedStatus = 'PENDING'
        ORDER BY p.id ASC
    """)
    List<Place> findTop5ByEmbedPending(Pageable pageable);

    default List<Place> findTop5ByEmbedPending() {
        return findTop5ByEmbedPending(Pageable.ofSize(5));
    }

    /**
     * Find places for vector embedding batch processing
     * Conditions: crawl_status = COMPLETED, embed_status = PENDING, mohe_description IS NOT NULL
     */
    @Query("""
        SELECT p FROM Place p
        JOIN p.descriptions d
        WHERE p.crawlStatus = 'COMPLETED'
        AND p.embedStatus = 'PENDING'
        AND d.moheDescription IS NOT NULL
        AND d.moheDescription != ''
        ORDER BY p.id ASC
    """)
    Page<Place> findPlacesForVectorEmbedding(Pageable pageable);

    /**
     * Find place IDs for keyword embedding batch processing
     * Step 1: Get IDs with pagination (efficient)
     * Conditions: crawl_status = COMPLETED
     */
    @Query("""
        SELECT p.id FROM Place p
        WHERE p.crawlStatus = 'COMPLETED'
        ORDER BY p.id ASC
    """)
    Page<Long> findPlaceIdsForKeywordEmbedding(Pageable pageable);

    /**
     * Find a single Place by ID (for keyword embedding)
     * Step 2: Load entity - no need to fetch collections for keyword embedding
     * We only need the keyword field from the Place entity
     */
    @Query("SELECT p FROM Place p WHERE p.id = :id")
    Optional<Place> findByIdForKeywordEmbedding(@Param("id") Long id);

    /**
     * Find all place IDs for image refresh batch processing
     * Returns all place IDs ordered by ID for sequential processing
     */
    @Query("""
        SELECT p.id FROM Place p
        ORDER BY p.id ASC
    """)
    Page<Long> findAllPlaceIdsForImageRefresh(Pageable pageable);

    /**
     * Find place IDs that have no images or broken images for refresh
     * Returns IDs of places without images
     */
    @Query("""
        SELECT p.id FROM Place p
        WHERE p.id NOT IN (
            SELECT DISTINCT pi.place.id FROM PlaceImage pi
        )
        ORDER BY p.id ASC
    """)
    Page<Long> findPlaceIdsWithoutImages(Pageable pageable);

    /**
     * Find place IDs by embed status for selective image refresh
     */
    @Query("""
        SELECT p.id FROM Place p
        WHERE p.embedStatus = :status
        ORDER BY p.id ASC
    """)
    Page<Long> findPlaceIdsByEmbedStatus(@Param("status") EmbedStatus status, Pageable pageable);

    // ===== Admin Monitor Stats =====

    /**
     * Count total places
     */
    long count();

    /**
     * Count places where embed_status = COMPLETED
     */
    long countByEmbedStatus(EmbedStatus status);

    /**
     * Count places where crawl_status = COMPLETED
     */
    long countByCrawlStatus(CrawlStatus status);

    /**
     * Search places by name with status filters for admin
     */
    @Query("""
        SELECT p FROM Place p
        WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:status = 'all'
            OR (:status = 'embedded' AND p.embedStatus = 'COMPLETED')
            OR (:status = 'crawled' AND p.crawlStatus = 'COMPLETED')
            OR (:status = 'pending' AND p.crawlStatus = 'PENDING')
            OR (:status = 'failed' AND p.crawlStatus = 'FAILED')
            OR (:status = 'not_found' AND p.crawlStatus = 'NOT_FOUND'))
        ORDER BY p.updatedAt DESC
    """)
    Page<Place> searchPlacesForAdmin(
        @Param("keyword") String keyword,
        @Param("status") String status,
        Pageable pageable
    );
}
