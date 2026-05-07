package com.example.scenic_rag_system.repository;

import com.example.scenic_rag_system.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
