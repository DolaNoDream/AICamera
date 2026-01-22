package com.example.aicamerabackend.controller;

import com.example.aicamerabackend.dto.PoseSugResponse;
import com.example.aicamerabackend.service.GuidanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/*
帧分析接口
输入：sessionId（当前会话）, image（图片文件）, meta（可选，元数据）
输出：FrameAnalyzeResponse（分析结果）
 */
@RestController
@RequiredArgsConstructor
public class FrameController {

    private final GuidanceService guidanceService;
    @PostMapping(value = "/api/frame/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PoseSugResponse analyzeFrame(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "userIntent", required = false) String userIntent,
            @RequestParam(value = "meta", required = false) String meta
    ) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("image is required");
        }

        System.out.println("sessionId=" + sessionId
                + ", filename=" + image.getOriginalFilename()
                + ", size=" + image.getSize()
                + ", userIntent=" + userIntent
                + ", meta=" + meta);

        return guidanceService.analyzeFrame(image, sessionId, userIntent, meta);
    }
}
