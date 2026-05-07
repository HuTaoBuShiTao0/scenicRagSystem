package com.example.scenic_rag_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorDocument {
    private String id;
    private List<Double> embedding;
    private String text;
    private Map<String, Object> metadata;
    private Double score;
}
