package com.example.aicamera.network.model;

import com.google.gson.annotations.SerializedName;

public class PoseDetails {
    @SerializedName("head") public String head;
    @SerializedName("arms") public String arms;
    @SerializedName("hands") public String hands;
    @SerializedName("torso") public String torso;
    @SerializedName("hips") public String hips;
    @SerializedName("legs") public String legs;
    @SerializedName("feet") public String feet;
    @SerializedName("orientation") public String orientation;

    // Getters can be added here if needed
}
