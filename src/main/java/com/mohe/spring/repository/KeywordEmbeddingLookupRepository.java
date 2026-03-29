package com.mohe.spring.repository;

import com.mohe.spring.entity.KeywordEmbeddingLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for global keyword embedding lookup cache.
 */
@Repository
public interface KeywordEmbeddingLookupRepository extends JpaRepository<KeywordEmbeddingLookup, Long> {

    Optional<KeywordEmbeddingLookup> findByKeyword(String keyword);

    List<KeywordEmbeddingLookup> findByKeywordIn(List<String> keywords);
}
