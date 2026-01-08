package com.example.aicamerabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//帧分析结果响应DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrameAnalyzeResponse {
    private String sessionId;  //分析会话唯一标识
    private List<PoseSuggestion> poseSuggestions; //姿势建议DTO列表
    private Overlay overlay; //叠加信息DTO
    private String voiceGuideText; //  TTS（语音）的文案
    private String audioUrl; // TTS 合成后的音频地址（可空）
}
