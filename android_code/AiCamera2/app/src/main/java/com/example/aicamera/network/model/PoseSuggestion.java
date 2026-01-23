package com.example.aicamera.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PoseSuggestion {
    @SerializedName("id") public String id;
    @SerializedName("name") public String name;
    @SerializedName("priority") public int priority;
    @SerializedName("details") public PoseDetails details;
    @SerializedName("tips") public List<String> tips;

    // Getters...
}
