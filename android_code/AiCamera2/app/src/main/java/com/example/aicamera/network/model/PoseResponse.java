package com.example.aicamera.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PoseResponse {
    @SerializedName("sessionId") public String sessionId;
    @SerializedName("poseImageUrl") public String poseImageUrl;
    @SerializedName("guideText") public String guideText;
    @SerializedName("voiceAudioText") public String voiceAudioText;
    @SerializedName("poseSuggestions") public List<PoseSuggestion> poseSuggestions;

    // Getters...
}