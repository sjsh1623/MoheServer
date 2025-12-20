package com.mohe.spring.repository;

import com.mohe.spring.entity.PlaceMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 장소 메뉴 Repository
 */
@Repository
public interface PlaceMenuRepository extends JpaRepository<PlaceMenu, Long> {

    /**
     * 장소 ID로 메뉴 조회 (표시 순서 정렬)
     */
    List<PlaceMenu> findByPlaceIdOrderByDisplayOrderAsc(Long placeId);

    /**
     * 장소 ID로 메뉴 조회
     */
    List<PlaceMenu> findByPlaceId(Long placeId);

    /**
     * 장소 ID와 메뉴명으로 메뉴 조회
     */
    @Query("SELECT pm FROM PlaceMenu pm WHERE pm.place.id = :placeId AND pm.name = :name")
    List<PlaceMenu> findByPlaceIdAndName(@Param("placeId") Long placeId, @Param("name") String name);

    /**
     * 장소의 메뉴 개수 조회
     */
    @Query("SELECT COUNT(pm) FROM PlaceMenu pm WHERE pm.place.id = :placeId")
    long countByPlaceId(@Param("placeId") Long placeId);

    /**
     * 장소의 모든 메뉴 삭제
     */
    @Modifying
    @Query("DELETE FROM PlaceMenu pm WHERE pm.place.id = :placeId")
    void deleteByPlaceId(@Param("placeId") Long placeId);

    /**
     * 인기 메뉴만 조회
     */
    @Query("SELECT pm FROM PlaceMenu pm WHERE pm.place.id = :placeId AND pm.isPopular = true ORDER BY pm.displayOrder ASC")
    List<PlaceMenu> findPopularMenusByPlaceId(@Param("placeId") Long placeId);

    /**
     * 이미지 다운로드 대기 중인 메뉴 조회
     */
    @Query("SELECT pm FROM PlaceMenu pm WHERE pm.imageUrl IS NOT NULL AND pm.imagePath IS NULL")
    List<PlaceMenu> findMenusWithPendingImageDownload();
}
