package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.entity.Prompt;
import com.example.scenic_rag_system.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提示词管理控制器
 * 管理员可对系统中的提示词进行增删改查，修改后实时生效（清除本地缓存）
 */
@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptRepository promptRepository;

    /**
     * 提示词内存缓存，修改时清除对应条目，下次读取时重新加载
     */
    public static final Map<String, String> promptCache = new ConcurrentHashMap<>();

    // ============= CRUD =============

    @GetMapping
    public Result<?> listAll() {
        List<Prompt> prompts = promptRepository.findAll();
        return Result.success(prompts);
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return promptRepository.findById(id)
                .map(Result::success)
                .orElse(Result.error(404, "提示词不存在"));
    }

    /**
     * 按类型获取提示词（前端编辑用）
     */
    @GetMapping("/type/{type}")
    public Result<?> getByType(@PathVariable String type) {
        return promptRepository.findByType(type)
                .map(Result::success)
                .orElse(Result.error(404, "未找到类型为 " + type + " 的提示词"));
    }

    @PostMapping
    public Result<?> create(@RequestBody Prompt prompt) {
        if (prompt.getType() == null || prompt.getType().isBlank()) {
            return Result.error(400, "提示词类型不能为空");
        }
        if (promptRepository.findByType(prompt.getType()).isPresent()) {
            return Result.error(400, "提示词类型 '" + prompt.getType() + "' 已存在");
        }
        if (prompt.getEnabled() == null) {
            prompt.setEnabled(true);
        }
        if (prompt.getVersion() == null) {
            prompt.setVersion(1);
        }
        Prompt saved = promptRepository.save(prompt);
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Prompt updated) {
        return promptRepository.findById(id)
                .map(existing -> {
                    // 如果变更了 type，检查是否与其他记录冲突
                    if (updated.getType() != null && !updated.getType().equals(existing.getType())) {
                        Optional<Prompt> conflict = promptRepository.findByType(updated.getType());
                        if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
                            return Result.<Prompt>error(400, "提示词类型 '" + updated.getType() + "' 已存在");
                        }
                        existing.setType(updated.getType());
                    }
                    if (updated.getName() != null) existing.setName(updated.getName());
                    if (updated.getContent() != null) {
                        existing.setContent(updated.getContent());
                        existing.setVersion(existing.getVersion() + 1);
                    }
                    if (updated.getEnabled() != null) existing.setEnabled(updated.getEnabled());
                    if (updated.getRemark() != null) existing.setRemark(updated.getRemark());

                    Prompt saved = promptRepository.save(existing);
                    // 清除缓存，下次读取时重新加载
                    promptCache.remove(saved.getType());
                    return Result.<Prompt>success(saved);
                })
                .orElse(Result.error(404, "提示词不存在"));
    }

    /**
     * 快捷更新提示词内容（仅修改 content，清缓存）
     */
    @PutMapping("/{id}/content")
    public Result<?> updateContent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null) {
            return Result.error(400, "内容不能为空");
        }
        return promptRepository.findById(id)
                .map(existing -> {
                    existing.setContent(content);
                    existing.setVersion(existing.getVersion() + 1);
                    Prompt saved = promptRepository.save(existing);
                    promptCache.remove(saved.getType());
                    return Result.<Prompt>success(saved);
                })
                .orElse(Result.error(404, "提示词不存在"));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return promptRepository.findById(id)
                .map(p -> {
                    promptCache.remove(p.getType());
                    promptRepository.delete(p);
                    return Result.<Prompt>success();
                })
                .orElse(Result.error(404, "提示词不存在"));
    }

    /**
     * 清除指定类型的缓存，用于外部触发（如手动刷新）
     */
    @PostMapping("/cache/evict/{type}")
    public Result<?> evictCache(@PathVariable String type) {
        promptCache.remove(type);
        return Result.success();
    }

    /**
     * 清除所有提示词缓存
     */
    @PostMapping("/cache/evict-all")
    public Result<?> evictAllCache() {
        promptCache.clear();
        return Result.success();
    }
}
