package com.example.aicamera;


import android.app.Application;
import android.util.Log;

import com.iflytek.sparkchain.core.SparkChain;
import com.iflytek.sparkchain.core.SparkChainConfig;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    // 统一配置 App 信息
    private static final String APP_ID = "c84a2dee";
    private static final String API_KEY = "a1ff35acf5c2c54c527a5ca478a7ccb9";
    private static final String API_SECRET = "ODg5NjA1MmUwNmE2ZDJiOGFlMTYyMGYy";

    private boolean isInit = false;

    @Override
    public void onCreate() {
        super.onCreate();
        initSparkChain();
    }

    private void initSparkChain() {
        // 配置全局参数
        SparkChainConfig config = SparkChainConfig.builder()
                .appID(APP_ID)
                .apiKey(API_KEY)
                .apiSecret(API_SECRET)
                .logLevel(0); // 0 为打印详细日志，方便调试

        // 执行全局唯一的初始化
        int ret = SparkChain.getInst().init(getApplicationContext(), config);

        if (ret == 0) {
            Log.d(TAG, "SparkChain 全局初始化成功");
            isInit = true;
        } else {
            Log.e(TAG, "SparkChain 全局初始化失败，错误码: " + ret);
        }
    }


}
