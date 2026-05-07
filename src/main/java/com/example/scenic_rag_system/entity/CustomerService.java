package com.example.scenic_rag_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kb_customer_service")
public class CustomerService extends BaseEntity {
    /** 关键词 */
    @Column(name = "keywords", length = 500)
    private String keywords;

    /** 回答内容 */
    @Column(name = "response", columnDefinition = "TEXT", nullable = false)
    private String response;
}
