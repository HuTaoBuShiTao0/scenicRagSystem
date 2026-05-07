package com.example.scenic_rag_system.repository;

import com.example.scenic_rag_system.entity.ScenicArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScenicAreaRepository extends JpaRepository<ScenicArea, Long> {
    List<ScenicArea> findByNameContaining(String name);
}
