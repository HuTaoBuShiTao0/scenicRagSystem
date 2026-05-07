package com.example.scenic_rag_system;

import com.example.scenic_rag_system.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class ScenicRagSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScenicRagSystemApplication.class, args);
    }

    @Bean
    CommandLineRunner init(KnowledgeService knowledgeService) {
        return args -> {
            log.info("===== 洛阳景区问答智能体 启动完成 =====");
            log.info("知识库类型: 景区, 景点, 美食, 酒店, 特产, 门票, 固定回答, 客服回答");
            log.info("向量数据库: Chroma");
            log.info("业务数据库: MySQL");
            log.info("缓存: Redis");
            log.info("LLM: 智谱GLM-4V-Plus");
            log.info("===================================");

            // 首次启动时同步数据到向量库
            try {
                knowledgeService.syncAllToVector();
                log.info("向量库数据同步完成");
            } catch (Exception e) {
                log.warn("向量库同步失败（首次启动可忽略）: {}", e.getMessage());
            }
        };
    }
}
