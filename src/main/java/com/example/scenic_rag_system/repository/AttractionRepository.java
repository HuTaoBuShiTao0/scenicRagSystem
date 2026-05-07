package com.example.scenic_rag_system.repository;

import com.example.scenic_rag_system.entity.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Long> {
    List<Attraction> findByNameContaining(String name);
    List<Attraction> findByRelatedScenicContaining(String relatedScenic);
}
