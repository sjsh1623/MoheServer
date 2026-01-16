package com.mohe.spring.dto;

import java.util.List;

/**
 * 통합 검색 응답 DTO
 */
public class UnifiedSearchResponse {

    private List<SimplePlaceDto> places;
    private int totalResults;
    private String query;
    private String searchType; // hybrid, embedding, keyword, none
    private long searchTimeMs;
    private String message;

    public UnifiedSearchResponse() {}

    public UnifiedSearchResponse(List<SimplePlaceDto> places, int totalResults, String query,
                                 String searchType, long searchTimeMs, String message) {
        this.places = places;
        this.totalResults = totalResults;
        this.query = query;
        this.searchType = searchType;
        this.searchTimeMs = searchTimeMs;
        this.message = message;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<SimplePlaceDto> places;
        private int totalResults;
        private String query;
        private String searchType;
        private long searchTimeMs;
        private String message;

        public Builder places(List<SimplePlaceDto> places) {
            this.places = places;
            return this;
        }

        public Builder totalResults(int totalResults) {
            this.totalResults = totalResults;
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder searchType(String searchType) {
            this.searchType = searchType;
            return this;
        }

        public Builder searchTimeMs(long searchTimeMs) {
            this.searchTimeMs = searchTimeMs;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public UnifiedSearchResponse build() {
            return new UnifiedSearchResponse(places, totalResults, query, searchType, searchTimeMs, message);
        }
    }

    // Getters and Setters
    public List<SimplePlaceDto> getPlaces() {
        return places;
    }

    public void setPlaces(List<SimplePlaceDto> places) {
        this.places = places;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public long getSearchTimeMs() {
        return searchTimeMs;
    }

    public void setSearchTimeMs(long searchTimeMs) {
        this.searchTimeMs = searchTimeMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
