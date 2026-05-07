package com.example.scenic_rag_system.repository;

import com.example.scenic_rag_system.entity.CustomerService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerServiceRepository extends JpaRepository<CustomerService, Long> {
}
