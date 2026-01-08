package com.example.aicamerabackend.controller;

import com.example.aicamerabackend.dto.FrameAnalyzeResponse;
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
    //接口映射
    /*
    请求方式：POST
    请求路径：/api/frame/analyze
    请求类型：multipart/form-data
     */
    @PostMapping(value = "/api/frame/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)

    public FrameAnalyzeResponse analyzeFrame(
            @RequestParam("sessionId") String sessionId, // 会话ID
            @RequestParam("image") MultipartFile image, // 图片文件
            @RequestParam(value = "meta", required = false) String meta //预留可选参数
    ) {
        // 1) 基本校验
        if (image == null || image.isEmpty()) {// 前端没传图片
            throw new IllegalArgumentException("image is required");
        }

        // 2) 打印日志：确认收到图片（调试用）
        System.out.println("sessionId=" + sessionId
                + ", filename=" + image.getOriginalFilename()
                + ", size=" + image.getSize()
                + ", meta=" + meta);

        // 3) 先返回 mock（后面替换成调用 Python）
        //return guidanceService.analyzeFrameMock(sessionId);
        return guidanceService.analyzeFrame(image, sessionId);
    }
}
