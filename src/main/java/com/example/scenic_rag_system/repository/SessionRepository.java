package com.example.scenic_rag_system.repository;

import com.example.scenic_rag_system.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Query("SELECT s FROM Session s WHERE s.userId = :userId AND EXISTS (SELECT 1 FROM Message m WHERE m.sessionId = s.id) ORDER BY s.updatedAt DESC")
    List<Session> findByUserIdWithMessages(@Param("userId") Long userId);

    @Query(value = "SELECT DISTINCT s FROM Session s " +
           "JOIN Message m ON m.sessionId = s.id " +
           "WHERE s.userId = :userId " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "  s.title LIKE %:keyword% OR " +
           "  m.content LIKE %:keyword%)",
           countQuery = "SELECT COUNT(DISTINCT s.id) FROM Session s " +
           "JOIN Message m ON m.sessionId = s.id " +
           "WHERE s.userId = :userId " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "  s.title LIKE %:keyword% OR " +
           "  m.content LIKE %:keyword%)")
    Page<Session> searchUserSessions(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT DISTINCT s FROM Session s " +
           "LEFT JOIN User u ON s.userId = u.id " +
           "JOIN Message m ON m.sessionId = s.id " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR " +
           "  u.username LIKE %:keyword% OR " +
           "  u.nickname LIKE %:keyword% OR " +
           "  m.content LIKE %:keyword%)",
           countQuery = "SELECT COUNT(DISTINCT s.id) FROM Session s " +
           "LEFT JOIN User u ON s.userId = u.id " +
           "JOIN Message m ON m.sessionId = s.id " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR " +
           "  u.username LIKE %:keyword% OR " +
           "  u.nickname LIKE %:keyword% OR " +
           "  m.content LIKE %:keyword%)")
    Page<Session> adminSearchSessions(@Param("keyword") String keyword, Pageable pageable);
}
