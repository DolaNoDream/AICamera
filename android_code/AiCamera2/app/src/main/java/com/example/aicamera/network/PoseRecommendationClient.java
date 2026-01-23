package com.example.aicamera.network;


import android.util.Log;
import com.example.aicamera.network.model.PoseResponse;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PoseRecommendationClient {

    private static final String BASE_URL = "http://1.95.125.238:9001/"; //
    private static PoseRecommendationClient instance;
    private final PoseApiService apiService;

    // 1. 定义回调接口，供外部接收结果
    public interface PoseCallback {
        void onSuccess(PoseResponse response);
        void onError(String errorMessage);
    }

    // 2. 私有构造函数，初始化 Retrofit
    private PoseRecommendationClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(PoseApiService.class);
    }

    // 3. 单例模式获取实例
    public static synchronized PoseRecommendationClient getInstance() {
        if (instance == null) {
            instance = new PoseRecommendationClient();
        }
        return instance;
    }

    /**
     * 调用姿势分析接口
     *
     * @param imageFile  待分析的图片文件 (Required) [cite: 9]
     * @param userIntent 用户意图，如"显腿长" (Optional) [cite: 9]
     * @param metaJson   相机元数据 JSON (Optional) [cite: 9]
     * @param callback   结果回调
     */
    public void analyzePose(File imageFile, String userIntent, String metaJson, PoseCallback callback) {
        if (imageFile == null || !imageFile.exists()) {
            callback.onError("Image file does not exist");
            return;
        }

        // A. 构建 sessionId (必填) [cite: 9]
        String uuid = UUID.randomUUID().toString();
        RequestBody sessionIdBody = RequestBody.create(MediaType.parse("text/plain"), uuid);

        // B. 构建图片部分 (必填) [cite: 9]
        // 假设是 jpg/png，统一使用 image/*
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        // C. 构建可选参数
        RequestBody intentBody = null;
        if (userIntent != null && !userIntent.isEmpty()) {
            intentBody = RequestBody.create(MediaType.parse("text/plain"), userIntent);
        }

        RequestBody metaBody = null;
        if (metaJson != null && !metaJson.isEmpty()) {
            metaBody = RequestBody.create(MediaType.parse("text/plain"), metaJson);
        }

        // D. 发起异步请求
        Call<PoseResponse> call = apiService.uploadImage(sessionIdBody, imagePart, intentBody, metaBody);
        call.enqueue(new Callback<PoseResponse>() {
            @Override
            public void onResponse(Call<PoseResponse> call, Response<PoseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // HTTP 200 OK [cite: 12]
                    callback.onSuccess(response.body());
                } else {
                    // 错误处理 [cite: 13]
                    callback.onError("Request failed with code: " + response.code() + ", message: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PoseResponse> call, Throwable t) {
                // 网络错误
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}

