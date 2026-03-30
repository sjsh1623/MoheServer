package com.mohe.spring.repository;

import com.mohe.spring.entity.SearchConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchConversationRepository extends JpaRepository<SearchConversation, Long> {

    List<SearchConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<SearchConversation> findBySessionIdOrderByUpdatedAtDesc(String sessionId);

    List<SearchConversation> findTop20ByUserIdOrderByUpdatedAtDesc(Long userId);

    List<SearchConversation> findTop20BySessionIdOrderByUpdatedAtDesc(String sessionId);
}
