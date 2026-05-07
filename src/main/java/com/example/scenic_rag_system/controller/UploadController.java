package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Slf4j
public class UploadController {

    @Value("${spring.application.name}")
    private String appName;

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只能上传图片文件");
        }

        // 校验文件大小 (2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过2MB");
        }

        try {
            // 生成文件名
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + ext;

            // 保存到上传目录
            String uploadDir = System.getProperty("user.dir") + "/uploads/images/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(uploadDir + fileName);
            file.transferTo(dest);

            String url = "/uploads/images/" + fileName;
            return Result.success(Map.of("url", url));
        } catch (IOException e) {
            log.error("Upload failed", e);
            return Result.error(500, "上传失败");
        }
    }
}
