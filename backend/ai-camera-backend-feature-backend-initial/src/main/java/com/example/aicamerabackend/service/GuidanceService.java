package com.example.aicamerabackend.service;

import com.example.aicamerabackend.client.*;
import com.example.aicamerabackend.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
@RequiredArgsConstructor
public class GuidanceService {

    private final PoseAiClient poseAiClient;
    private final VoiceAiClient voiceAiClient;


    //画面分析
    /*
    输入：sessionId（当前会话）
    输出：FrameAnalyzeResponse（分析结果）
    */
    public FrameAnalyzeResponse analyzeFrameMock(String sessionId) {
        //构造姿势建议列表
        List<PoseSuggestion> poses = List.of( //mock 模拟一次画面分析
                new PoseSuggestion("p1", "侧身抬手", 1,
                        new String[]{"身体侧 30°", "手抬到额头附近", "肩放松"}),
                new PoseSuggestion("p2", "微仰头看镜头上方", 2,
                        new String[]{"下巴抬一点", "眼神看镜头上方", "嘴角轻微上扬"}),
                new PoseSuggestion("p3", "交叉腿显腿长", 3,
                        new String[]{"双腿交叉", "重心放后腿", "前脚尖点地"})
        );

        //构造画面叠加
        Overlay overlay = new Overlay(
                "推荐姿势：侧身抬手。身体侧一点，右手抬到额头附近，肩放松。",
                "https://example.com/mock/pose_p1.png"
        );

        String voiceGuideText = "好，现在身体侧一点点，肩放松，右手抬到额头附近，保持两秒。";

        return new FrameAnalyzeResponse(sessionId, poses, overlay, voiceGuideText,null);
    }

    public FrameAnalyzeResponse analyzeFrame(MultipartFile image, String sessionId) {
        FrameAnalyzeResponse resp;
        try {
            resp = poseAiClient.analyze(image, sessionId);
        } catch (Exception e) {
            System.out.println("Pose AI failed, fallback to mock. reason=" + e.getMessage());
            resp = analyzeFrameMock(sessionId);
        }

        // 调 TTS：把 voiceGuideText 合成语音
        try {
            String audioUrl = voiceAiClient.tts(sessionId, resp.getVoiceGuideText());
            resp.setAudioUrl(audioUrl);
        } catch (Exception e) {
            System.out.println("TTS failed, keep audioUrl null. reason=" + e.getMessage());
            resp.setAudioUrl(null);
        }

        return resp;
    }

    //语音处理
    /*
    输入：sessionId（当前会话），text（用户输入的语音文本）
    输出：VoiceTextResponse（处理结果）
     */
    public VoiceTextResponse handleVoiceTextMock(String sessionId, String text) {
        String reply = "收到，你的需求是：「" + text + "」。我先给你 3 个最适合的姿势，你想要偏正式还是偏休闲？";
        String voice = "好的，我明白了。我先给你三个姿势建议，你想要偏正式还是偏休闲？";

        String audioUrl = null;
        try {
            audioUrl = voiceAiClient.tts(sessionId, voice);
        } catch (Exception e) {
            System.out.println("TTS failed, keep audioUrl null. reason=" + e.getMessage());
        }

        return new VoiceTextResponse(sessionId, reply, voice, audioUrl);
    }
    public VoiceTextResponse handleVoiceText(String sessionId, String text) {
        // 1) 调 Python chat
        ChatResponse chatResp = voiceAiClient.chat(sessionId, text);

        String replyText = chatResp == null ? "我没听清，你可以再说一遍吗？" : chatResp.getReplyText();
        String voiceText = (chatResp == null || chatResp.getVoiceText() == null || chatResp.getVoiceText().isBlank())
                ? replyText
                : chatResp.getVoiceText();

        // 2) 再调 TTS
        String audioUrl = null;
        try {
            audioUrl = voiceAiClient.tts(sessionId, voiceText);
        } catch (Exception e) {
            System.out.println("TTS failed, keep audioUrl null. reason=" + e.getMessage());
        }

        return new VoiceTextResponse(sessionId, replyText, voiceText, audioUrl);
    }


}
