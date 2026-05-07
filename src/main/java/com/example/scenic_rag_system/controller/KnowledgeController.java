package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /**
     * 获取指定知识库的所有数据
     */
    @GetMapping("/{type}")
    public Result<?> list(@PathVariable String type) {
        try {
            return Result.success(knowledgeService.getAll(type));
        } catch (Exception e) {
            return Result.error(500, e.getMessage() != null ? e.getMessage() : "获取数据失败");
        }
    }

    /**
     * 添加数据
     */
    @PostMapping("/{type}")
    public Result<?> add(@PathVariable String type, @RequestBody Map<String, Object> data) {
        try {
            return Result.success(knowledgeService.add(type, data));
        } catch (Exception e) {
            return Result.error(500, e.getMessage() != null ? e.getMessage() : "添加数据失败");
        }
    }

    /**
     * 更新数据
     */
    @PutMapping("/{type}/{id}")
    public Result<?> update(@PathVariable String type, @PathVariable Long id,
                             @RequestBody Map<String, Object> data) {
        try {
            return Result.success(knowledgeService.update(type, id, data));
        } catch (Exception e) {
            return Result.error(500, e.getMessage() != null ? e.getMessage() : "更新数据失败");
        }
    }

    /**
     * 删除数据
     */
    @DeleteMapping("/{type}/{id}")
    public Result<?> delete(@PathVariable String type, @PathVariable Long id) {
        try {
            knowledgeService.delete(type, id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, e.getMessage() != null ? e.getMessage() : "删除数据失败");
        }
    }

    /**
     * 同步所有数据到向量库
     */
    @PostMapping("/sync-vector")
    public Result<?> syncVector() {
        knowledgeService.syncAllToVector();
        return Result.success();
    }
}
