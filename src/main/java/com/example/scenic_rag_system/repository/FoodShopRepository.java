package com.example.scenic_rag_system.repository;

import com.example.scenic_rag_system.entity.FoodShop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodShopRepository extends JpaRepository<FoodShop, Long> {
    List<FoodShop> findByNameContaining(String name);
    List<FoodShop> findByRelatedScenicContaining(String relatedScenic);
}
