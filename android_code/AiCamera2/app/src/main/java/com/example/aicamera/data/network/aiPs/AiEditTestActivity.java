package com.example.aicamera.data.network.aiPs;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.aicamera.R;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.UUID;

public class AiEditTestActivity extends AppCompatActivity {
    // 日志标签（方便查看调试信息）
    private static final String TAG = "AiEditTest";
    // 你的应用包名（替换为实际值）
    //private static final String APP_PACKAGE_NAME = "com.example.aieditdemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 第一步：准备测试参数
        // 生成唯一sessionId
        String sessionId = UUID.randomUUID().toString();
        // 测试图片路径（对应前面放图片的位置）
        File testImageFile = new File(getExternalFilesDir("images"), "2.jpg");
        // 修图需求（自定义测试参数）
        PictureRequirement requirement = new PictureRequirement();
        requirement.setFilter("复古"); // 滤镜
        requirement.setSpecial("去掉图片中的人群"); // 特殊要求

        // 2. 第二步：检查测试图片是否存在（先验证文件路径）
        if (!testImageFile.exists()) {
            String errorMsg = "测试图片不存在！路径：" + testImageFile.getAbsolutePath();
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorMsg);
            return;
        }
        Log.d(TAG, "测试图片路径：" + testImageFile.getAbsolutePath());

        // 3. 第三步：初始化工具类（单例）
        AiEditManager aiEditManager = AiEditManager.getInstance(getApplication());

        // 4. 第四步：调用工具类核心方法，测试功能
        aiEditManager.editImage(sessionId, testImageFile, requirement, result -> {
            // 切回主线程更新UI（网络操作在子线程）
            runOnUiThread(() -> {
                // 5. 第五步：验证结果
                if (result.isSuccess()) {
                    // ✅ 测试成功：打印日志+Toast提示
                    String savePath = result.getImageSavePath();
                    Toast.makeText(AiEditTestActivity.this,
                            "工具类测试成功！图片保存路径：\n" + savePath,
                            Toast.LENGTH_LONG).show();
                    Log.d(TAG, "测试成功 → 图片保存路径：" + savePath);

                    // 可选：验证文件是否真的保存成功
                    File savedImage = new File(savePath);
                    if (savedImage.exists()) {
                        Log.d(TAG, "验证：图片文件已保存，大小：" + savedImage.length() + " 字节");
                    } else {
                        Log.w(TAG, "验证：图片路径存在，但文件未找到！");
                    }
                } else {
                    // ❌ 测试失败：打印错误日志+Toast提示
                    String errorMsg = result.getErrorMsg();
                    Toast.makeText(AiEditTestActivity.this,
                            "工具类测试失败：" + errorMsg,
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "测试失败 → 原因：" + errorMsg);
                }
            });
        });
    }
}