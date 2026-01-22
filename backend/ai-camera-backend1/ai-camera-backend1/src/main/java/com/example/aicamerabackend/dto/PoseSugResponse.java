package com.example.aicamerabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoseSugResponse {
    private String sessionId;
    private String poseImageUrl;
    private String guideText;
    private String voiceAudioText;
    private List<PoseSuggestion> poseSuggestions;
}
