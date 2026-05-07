package com.example.scenic_rag_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kb_fixed_qa")
public class FixedQA extends BaseEntity {
    /** 问题 (RAG可检索) */
    @Column(name = "question", length = 500, nullable = false)
    private String question;

    /** 回答 */
    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;
}
