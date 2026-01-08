package com.example.aicamerabackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//用于接收语音引导文本
@Data
public class VoiceTextRequest {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String text;
}
