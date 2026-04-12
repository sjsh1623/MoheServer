package com.mohe.spring.service;

import com.mohe.spring.dto.SimplePlaceDto;
import com.mohe.spring.dto.UnifiedSearchResponse;
import com.mohe.spring.entity.SearchConversation;
import com.mohe.spring.entity.SearchMessage;
import com.mohe.spring.repository.SearchConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchChatService {

    private final UnifiedSearchService unifiedSearchService;
    private final SearchConversationRepository conversationRepository;

    /**
     * 검색 + 대화 저장 통합
     */
    @Transactional
    public Map<String, Object> searchAndSave(
            String query, Long conversationId, Long userId, String sessionId,
            Double latitude, Double longitude, int limit) {

        // 1. 검색 실행
        UnifiedSearchResponse searchResult = unifiedSearchService.searchSemantic(query, latitude, longitude, limit);

        // 2. 대화 생성 또는 기존 대화에 추가
        SearchConversation conversation;
        if (conversationId != null) {
            conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) {
                conversation = createConversation(userId, sessionId, query);
            }
        } else {
            conversation = createConversation(userId, sessionId, query);
        }

        // 3. 사용자 메시지 저장
        SearchMessage userMsg = new SearchMessage(conversation, "user", query, null);
        conversation.getMessages().add(userMsg);

        // 4. AI 응답 메시지 저장
        Long[] placeIds = searchResult.getPlaces().stream()
                .map(p -> Long.parseLong(p.getId()))
                .toArray(Long[]::new);

        String aiMessage = searchResult.getMessage();
        SearchMessage assistantMsg = new SearchMessage(conversation, "assistant", aiMessage, placeIds);
        conversation.getMessages().add(assistantMsg);

        conversationRepository.save(conversation);

        // 5. 응답 구성
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conversation.getId());
        result.put("message", aiMessage);
        result.put("places", searchResult.getPlaces());
        result.put("totalResults", searchResult.getTotalResults());
        result.put("searchType", searchResult.getSearchType());
        result.put("searchTimeMs", searchResult.getSearchTimeMs());

        return result;
    }

    /**
     * 대화 목록 조회
     */
    public List<Map<String, Object>> getConversations(Long userId, String sessionId) {
        List<SearchConversation> conversations;
        if (userId != null) {
            conversations = conversationRepository.findTop20ByUserIdOrderByUpdatedAtDesc(userId);
        } else if (sessionId != null) {
            conversations = conversationRepository.findTop20BySessionIdOrderByUpdatedAtDesc(sessionId);
        } else {
            return List.of();
        }

        return conversations.stream().map(conv -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", conv.getId());
            item.put("title", conv.getTitle());
            item.put("updatedAt", conv.getUpdatedAt().toString());
            item.put("messageCount", conv.getMessages().size());

            // 마지막 응답의 장소 미리보기
            conv.getMessages().stream()
                .filter(m -> "assistant".equals(m.getRole()) && m.getPlaceIds() != null)
                .reduce((a, b) -> b) // last
                .ifPresent(lastMsg -> item.put("placeCount", lastMsg.getPlaceIds().length));

            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 대화 상세 조회
     */
    public Map<String, Object> getConversation(Long conversationId) {
        SearchConversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv == null) return null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", conv.getId());
        result.put("title", conv.getTitle());
        result.put("createdAt", conv.getCreatedAt().toString());

        List<Map<String, Object>> messages = conv.getMessages().stream().map(msg -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            m.put("placeIds", msg.getPlaceIds());
            m.put("createdAt", msg.getCreatedAt().toString());
            return m;
        }).collect(Collectors.toList());

        result.put("messages", messages);
        return result;
    }

    /**
     * 대화 삭제
     */
    @Transactional
    public boolean deleteConversation(Long conversationId) {
        if (conversationRepository.existsById(conversationId)) {
            conversationRepository.deleteById(conversationId);
            return true;
        }
        return false;
    }

    private SearchConversation createConversation(Long userId, String sessionId, String firstQuery) {
        SearchConversation conv = new SearchConversation();
        conv.setUserId(userId);
        conv.setSessionId(sessionId);
        // 제목: 첫 질의를 30자로 자름
        conv.setTitle(firstQuery.length() > 30 ? firstQuery.substring(0, 30) + "..." : firstQuery);
        return conv;
    }
}
