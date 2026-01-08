package com.example.aicamerabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//接口返回 DTO，用于将后端生成的文本回复和语音合成文案统一返回给前端
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTextResponse {
    private String sessionId;
    private String assistantReplyText; // 给前端显示的文字回复
    private String voiceReplyText;     // 未来交给 TTS 合成语音的文案
    private String audioUrl; // 新增：TTS 音频链接

}
