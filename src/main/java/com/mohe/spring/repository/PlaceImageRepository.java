package com.mohe.spring.repository;

import com.mohe.spring.entity.PlaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {
    @Query("SELECT pi FROM PlaceImage pi WHERE pi.place.id = :placeId ORDER BY pi.orderIndex ASC")
    List<PlaceImage> findByPlaceIdOrderByOrderIndexAsc(@Param("placeId") Long placeId);

    @Query("SELECT pi FROM PlaceImage pi WHERE pi.place.id = :placeId ORDER BY pi.orderIndex ASC")
    Optional<PlaceImage> findFirstByPlaceIdOrderByOrderIndexAsc(@Param("placeId") Long placeId);
}
