package com.example.scenic_rag_system.repository;

import com.example.scenic_rag_system.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);

    @Query("SELECT m.sessionId, COUNT(m) FROM Message m WHERE m.sessionId IN :sessionIds GROUP BY m.sessionId")
    List<Object[]> countBySessionIds(@Param("sessionIds") List<Long> sessionIds);
}
