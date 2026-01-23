package com.example.aicamera.network;

import com.example.aicamera.network.model.PoseResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface PoseApiService {
    // 接口地址: http://1.95.125.238:9001/posesug
    @Multipart
    @POST("posesug")
    Call<PoseResponse> uploadImage(
            @Part("sessionId") RequestBody sessionId,
            @Part MultipartBody.Part image,
            @Part("userIntent") RequestBody userIntent, // 可选
            @Part("meta") RequestBody meta              // 可选
    );
}

