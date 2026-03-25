package com.example.aicamera.data.network.aiPs;

import com.google.gson.Gson;
import okhttp3.*;
import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import android.util.Log;

/**
 * AI修图核心管理类（主程序仅需调用此类）
 * 封装所有内部逻辑，对外暴露单一调用入口
 */
public class AiEditManager {
    // 配置项（可根据实际修改）
    private static final String BASE_URL = "http://1.95.125.238:9001";
    private static final String PICTURE_API_PATH = "/ai/picture";
    private static final String SAVE_DIR_NAME = "AiEditedImages"; // 图片保存目录名
    //private static final String APP_PACKAGE_NAME = "com.xxx.aiedit"; // 替换为你的应用包名

    // 内部依赖（私有化，主程序无需关注）
    private static OkHttpClient okHttpClient;
    private static Gson gson;
    private Context appContext; // 应用上下文（用于获取私有目录）

    // 单例模式（避免重复创建）
    private static volatile AiEditManager instance;

    /**
     * 获取单例实例（主程序调用此方法初始化）
     * @param context 应用上下文（建议传Application Context）
     * @return 单例实例
     */
    public static AiEditManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AiEditManager.class) {
                if (instance == null) {
                    instance = new AiEditManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // 私有构造器（初始化内部依赖）
    private AiEditManager(Context context) {
        this.appContext = context;
        // 初始化OkHttp
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        // 初始化Gson
        gson = new Gson();
    }

    /**
     * 回调接口：主程序仅需实现此回调获取最终结果
     */
    public interface AiEditCallback {
        void onResult(AiEditResult result);
    }

    /**
     * 核心方法：主程序唯一需要调用的方法
     * 输入参数 → 内部自动完成「修图API调用+下载图片」→ 返回最终结果
     * @param sessionId    会话ID
     * @param imageFile    待修图的图片文件
     * @param requirement  修图需求
     * @param callback     结果回调（返回图片保存路径/错误信息）
     */
    public void editImage(String sessionId, File imageFile, PictureRequirement requirement, AiEditCallback callback) {
        // 1. 前置参数校验（失败直接返回）
        AiEditResult checkResult = checkParams(sessionId, imageFile, requirement);
        if (!checkResult.isSuccess()) {
            callback.onResult(checkResult);
            return;
        }

        // 2. 第一步：调用修图API获取图片URL
        callEditApi(sessionId, imageFile, requirement, new EditApiCallback() {
            @Override
            public void onApiSuccess(String imageUrl) {
                // 3. 第二步：下载图片到本地
                downloadImage(imageUrl, new DownloadCallback() {
                    @Override
                    public void onDownloadSuccess(String savePath) {
                        // 最终成功：返回图片保存路径
                        callback.onResult(AiEditResult.success(savePath));
                    }

                    @Override
                    public void onDownloadFailure(String errorMsg) {
                        // 下载失败
                        callback.onResult(AiEditResult.failure("图片下载失败：" + errorMsg));
                    }
                });
            }

            @Override
            public void onApiFailure(String errorMsg) {
                // 修图API调用失败
                callback.onResult(AiEditResult.failure("修图接口调用失败：" + errorMsg));
            }
        });
    }

    // ---------------------- 内部私有方法（主程序无需关注） ----------------------
    /**
     * 参数校验（内部方法）
     */
    private AiEditResult checkParams(String sessionId, File imageFile, PictureRequirement requirement) {
        if (sessionId == null || sessionId.isEmpty()) {
            return AiEditResult.failure("sessionId不能为空");
        }
        if (imageFile == null || !imageFile.exists()) {
            return AiEditResult.failure("图片文件不存在或路径错误");
        }
        if (requirement == null) {
            return AiEditResult.failure("修图需求不能为空");
        }
        // 校验需求至少有一个有效值
        boolean hasValidReq = (requirement.getFilter() != null && !requirement.getFilter().isEmpty())
                || (requirement.getPortrait() != null && !requirement.getPortrait().isEmpty())
                || (requirement.getBackground() != null && !requirement.getBackground().isEmpty())
                || (requirement.getSpecial() != null && !requirement.getSpecial().isEmpty());
        if (!hasValidReq) {
            return AiEditResult.failure("修图需求至少填写一项（滤镜/人像/背景/特殊要求）");
        }
        return AiEditResult.success(""); // 校验通过
    }

    /**
     * 调用修图API（内部方法）
     */
    private void callEditApi(String sessionId, File imageFile, PictureRequirement requirement, EditApiCallback callback) {
        // 构建Multipart请求体
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("sessionId", sessionId)
                .addFormDataPart("requirement", gson.toJson(requirement))
                .addFormDataPart("image", imageFile.getName(),
                        RequestBody.create(MediaType.parse("image/*"), imageFile));

        Request request = new Request.Builder()
                .url(BASE_URL + PICTURE_API_PATH)
                .post(multipartBuilder.build())
                .build();

        // 异步调用API
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onApiFailure("网络连接失败：" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onApiFailure("接口响应失败，状态码：" + response.code());
                    return;
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    callback.onApiFailure("接口响应体为空");
                    return;
                }

                try {
                    // 解析API返回的图片URL
                    String responseJson = responseBody.string();
                    Log.d("AiEditApiResponse", "API返回完整JSON：" + responseJson);

                    AiPictureApiResponse apiResponse = gson.fromJson(responseJson, AiPictureApiResponse.class);
                    if (apiResponse.getCode() != 200 || apiResponse.getImageUrl() == null || apiResponse.getImageUrl() == null) {
                        callback.onApiFailure("接口返回异常：" + apiResponse.getMsg());
                        return;
                    }
                    // 传递图片URL给下一步
                    callback.onApiSuccess(apiResponse.getImageUrl());
                } catch (Exception e) {
                    callback.onApiFailure("响应解析失败：" + e.getMessage());
                }
            }
        });
    }

    /**
     * 下载图片（内部方法）
     */
    private void downloadImage(String imageUrl, DownloadCallback callback) {
        Request request = new Request.Builder()
                .url(imageUrl)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onDownloadFailure("网络下载失败：" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onDownloadFailure("URL响应失败，状态码：" + response.code());
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    callback.onDownloadFailure("图片数据为空");
                    return;
                }

                // 保存图片到应用私有目录（无需权限）
                try {
                    // 获取应用私有存储目录（推荐，无需动态权限）
                    File saveDir = new File(appContext.getExternalFilesDir(null), SAVE_DIR_NAME);
                    if (!saveDir.exists()) {
                        saveDir.mkdirs();
                    }

                    // 生成唯一文件名
                    String fileName = "ai_edit_" + System.currentTimeMillis() + ".jpg";
                    File saveFile = new File(saveDir, fileName);

                    // 写入文件
                    InputStream inputStream = body.byteStream();
                    FileOutputStream outputStream = new FileOutputStream(saveFile);
                    byte[] buffer = new byte[1024 * 4];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.flush();
                    inputStream.close();
                    outputStream.close();

                    // 返回图片保存的绝对路径
                    callback.onDownloadSuccess(saveFile.getAbsolutePath());
                } catch (Exception e) {
                    callback.onDownloadFailure("文件保存失败：" + e.getMessage());
                }
            }
        });
    }

    // ---------------------- 内部回调接口（主程序无需关注） ----------------------
    /**
     * 修图API调用回调（内部使用）
     */
    private interface EditApiCallback {
        void onApiSuccess(String imageUrl);
        void onApiFailure(String errorMsg);
    }

    /**
     * 图片下载回调（内部使用）
     */
    private interface DownloadCallback {
        void onDownloadSuccess(String savePath);
        void onDownloadFailure(String errorMsg);
    }

    /**
     * 修图API响应解析类（内部使用，主程序无需关注）
     */
    private static class AiPictureApiResponse {
        private int code;          // 根节点code
        private String msg;        // 根节点msg
        private String imageUrl;   // 根节点imageUrl（关键修改）

        // Getter & Setter
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}