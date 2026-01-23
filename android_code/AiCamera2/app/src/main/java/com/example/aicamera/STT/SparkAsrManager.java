package com.example.aicamera.STT;



import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.iflytek.sparkchain.core.SparkChain;
import com.iflytek.sparkchain.core.SparkChainConfig;
import com.iflytek.sparkchain.core.asr.ASR;
import com.iflytek.sparkchain.core.asr.AsrCallbacks;
import com.iflytek.sparkchain.core.asr.AudioAttributes;

/**
 * 讯飞语音识别封装管理类
 */
public class SparkAsrManager extends Application {
    private static final String TAG = "SparkAsrManager";

    // 请替换为你自己的 Key
    private static final String APP_ID = "c84a2dee";
    private static final String API_KEY = "a1ff35acf5c2c54c527a5ca478a7ccb9";
    private static final String API_SECRET = "ODg5NjA1MmUwNmE2ZDJiOGFlMTYyMGYy";

    private static SparkAsrManager instance;
    private ASR mAsr;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private boolean isInited = false;

    // 回调接口
    public interface AsrListener {
        void onResult(String text, boolean isLast);
        void onError(int code, String msg);
    }

    private AsrListener listener;

    private SparkAsrManager() {}

    public static synchronized SparkAsrManager getInstance() {
        if (instance == null) instance = new SparkAsrManager();
        return instance;
    }

    /**
     * 初始化 SDK
     */
    public void init(Context context) {

        if (context == null) {
            Log.e(TAG, "初始化失败：传入的 Context 是空的！");
            return;
        }
        // 配置全局参数
        SparkChainConfig config = SparkChainConfig.builder()
                .appID(APP_ID)
                .apiKey(API_KEY)
                .apiSecret(API_SECRET)
                .logLevel(0); // 0 为打印详细日志，方便调试

        // 执行全局唯一的初始化
        int ret = SparkChain.getInst().init(context.getApplicationContext(), config);

        if (ret == 0) {
            Log.d(TAG, "SparkChain 全局初始化成功");

        } else {
            Log.e(TAG, "SparkChain 全局初始化失败，错误码: " + ret);
        }
        createASRInstance();
    }

    public boolean getInit(){
        return isInited;
    }

    private void createASRInstance() {
        mAsr = new ASR("zh_cn", "iat", "mandarin");
        mAsr.registerCallbacks(new AsrCallbacks() {
            @Override
            public void onResult(ASR.ASRResult result, Object o) {
                if (listener != null) {
                    listener.onResult(result.getBestMatchText(), true);
                }
            }

            @Override
            public void onError(ASR.ASRError error, Object o) {
                if (listener != null) {
                    listener.onError(error.getCode(), error.getErrMsg());
                }
            }

            @Override public void onBeginOfSpeech() {}
            @Override public void onRecordVolume(double v, int i) {}
        });
    }

    /**
     * 开始录音并识别
     */
    @SuppressLint("MissingPermission")
    public void startListening(AsrListener listener) {

        this.listener = listener;

        new Thread(() -> {
            // 1. 开启 ASR 会话
            AudioAttributes attr = new AudioAttributes();
            attr.setSampleRate(16000);
            attr.setEncoding("raw");
            attr.setChannels(1);
            attr.setBitdepth(16);
            mAsr.start(attr, null);

            // 2. 启动原生录音机
            int bufferSize = 1280;
            int minBufSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minBufSize, bufferSize));

            audioRecord.startRecording();
            isRecording = true;

            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int readSize = audioRecord.read(buffer, 0, buffer.length);
                if (readSize > 0 && mAsr != null) {
                    mAsr.write(buffer);
                }
            }
        }).start();
    }

    /**
     * 停止录音并结束识别
     */
    public void stopListening() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (mAsr != null) {
            mAsr.stop(false);
        }
    }

    public void destroy(){
        SparkChain.getInst().unInit();  //SDK逆初始化
    }
}
