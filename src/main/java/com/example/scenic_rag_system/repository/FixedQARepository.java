package com.example.scenic_rag_system.repository;

import com.example.scenic_rag_system.entity.FixedQA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FixedQARepository extends JpaRepository<FixedQA, Long> {
}
