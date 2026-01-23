package com.example.aicamera.TTS;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.iflytek.sparkchain.core.SparkChain;
import com.iflytek.sparkchain.core.tts.OnlineTTS;
import com.iflytek.sparkchain.core.tts.TTS;
import com.iflytek.sparkchain.core.tts.TTSCallbacks;


/**
 * 讯飞语音合成封装管理类
 */
public class TTSManager {
    private static final String TAG = "TTSManager";

    private static TTSManager instance;
    private OnlineTTS tts;
    private AudioTrack audioTrack;

    private boolean isTranslating = false;

    // 回调接口
    public interface TTSListener {
        void onResult(TTS.TTSResult result, Object usrTag);

        void onError(TTS.TTSError error, Object usrTag);
    }

    private TTSListener listener;

    public static synchronized TTSManager getInstance() {
        if (instance == null) instance = new TTSManager();
        return instance;
    }

    private TTSManager(){ }

    public void init(){
        createTTSInstance();
        initAudioTrack();
    }

    private void initAudioTrack(){
        int sampleRate = 16000; // 讯飞默认16k
        int minBufSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize,
                AudioTrack.MODE_STREAM);
    }


    public void createTTSInstance(){
        tts = new OnlineTTS("x4_xiaoyan");
        tts.aue("raw");

        TTSCallbacks mTTSCallback = new TTSCallbacks() {
            @Override
            public void onResult(TTS.TTSResult result, Object usrTag) {
                if(result != null && result.getData() != null){
                    byte[] data = result.getData();
                    Log.d(TAG,"收到数据长度"+data.length);

                    if(audioTrack != null){
                        if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack.play();
                        }
                        audioTrack.write(data, 0, data.length);
                    }
                }
                //解析获取的交互结果
                if(listener != null){
                    listener.onResult(result,usrTag);
                }
            }
            @Override
            public void onError(TTS.TTSError ttsError, Object usrTag) {
                if (listener != null){
                    listener.onError(ttsError, usrTag);
                }
            }
        };
        tts.registerCallbacks(mTTSCallback);
    }

    /**
     * 开始合成语音
     */
    public void startTTS(String text, TTSListener listener){

        this.listener = listener;
        if (audioTrack != null){
            audioTrack.flush();
        }

        new Thread(()->{
            tts.aRun(text);
            isTranslating = true;
        }
                ).start();
    }

    public void stopTTS(){
        isTranslating = false;
        if(tts != null){
            tts.stop();
        }
        if(audioTrack != null){
            audioTrack.pause();
            audioTrack.flush();
        }
    }

    public void destroy(){
        SparkChain.getInst().unInit();
    }
}
