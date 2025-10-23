package com.mohe.spring.service;

import com.mohe.spring.entity.PlaceKeywordEmbedding;
import com.mohe.spring.repository.PlaceKeywordEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for saving keyword embeddings with independent transactions
 * This ensures embeddings are committed immediately, separate from batch transactions
 */
@Service
@RequiredArgsConstructor
public class KeywordEmbeddingSaveService {

    private final PlaceKeywordEmbeddingRepository embeddingRepository;

    /**
     * Delete all embeddings for a place in a new transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteEmbeddingsForPlace(Long placeId) {
        embeddingRepository.deleteByPlaceId(placeId);
    }

    /**
     * Save a single embedding in a new transaction
     * This ensures the embedding is committed immediately
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PlaceKeywordEmbedding saveEmbedding(Long placeId, String keyword, float[] embedding) {
        PlaceKeywordEmbedding embeddingEntity = new PlaceKeywordEmbedding(
            placeId,
            keyword,
            embedding
        );
        return embeddingRepository.save(embeddingEntity);
    }

    /**
     * Save multiple embeddings in a single new transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveEmbeddings(Long placeId, List<String> keywords, List<float[]> embeddings) {
        int savedCount = 0;
        for (int i = 0; i < Math.min(keywords.size(), embeddings.size()); i++) {
            PlaceKeywordEmbedding embeddingEntity = new PlaceKeywordEmbedding(
                placeId,
                keywords.get(i),
                embeddings.get(i)
            );
            embeddingRepository.save(embeddingEntity);
            savedCount++;
        }
        return savedCount;
    }
}
