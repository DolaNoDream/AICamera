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

    /**
     * 调用 9001 子后端 /posesug 获取姿势推荐结果。
     * - ASR/TTS 均在前端完成，所以后端只返回 voiceAudioText（要播报的文字），不再生成 audioUrl。
     * - 若 9001 不可用，则 fallback 返回 mock，保证链路不崩。
     */
    public PoseSugResponse analyzeFrame(MultipartFile image,
                                        String sessionId,
                                        String userIntent,
                                        String meta) {
        try {
            return poseAiClient.poseSug(image, sessionId, userIntent, meta);
        } catch (Exception e) {
            System.out.println("Pose AI failed, fallback to mock. reason=" + e.getMessage());
            return analyzeFrameMock(sessionId, userIntent, meta);
        }
    }

    /**
     * fallback mock：9001 不通时也能让前端联调 UI/语音播报链路。
     */
    public PoseSugResponse analyzeFrameMock(String sessionId, String userIntent, String meta) {
        List<PoseSuggestion> suggestions = List.of(
                new PoseSuggestion("p1", "侧身抬手", 1,
                        new String[]{"身体侧 30°", "右手抬到额头附近", "肩放松"}),
                new PoseSuggestion("p2", "微仰头", 2,
                        new String[]{"下巴抬一点", "眼神看镜头上方", "自然微笑"}),
                new PoseSuggestion("p3", "交叉腿显腿长", 3,
                        new String[]{"双腿交叉", "重心放后腿", "前脚尖点地"})
        );

        String guideText = "（mock）推荐姿势：侧身抬手。身体侧一点，右手抬到额头附近，肩放松。";
        if (userIntent != null && !userIntent.isBlank()) {
            guideText += " 你的意图是：「" + userIntent + "」。";
        }

        String voiceAudioText = "（mock）好，现在身体侧一点点，肩放松，右手抬到额头附近，保持两秒。";

        // mock 示意图 URL：你们后续可以换成自己的静态资源/CDN
        String poseImageUrl = "https://example.com/mock/pose_p1.png";

        return new PoseSugResponse(
                sessionId,
                poseImageUrl,
                guideText,
                voiceAudioText,
                suggestions
        );
    }

    //画面分析
    /*
    输入：sessionId（当前会话）
    输出：FrameAnalyzeResponse（分析结果）
    */
    /*
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
    }*/
    /*
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
    }*/



}
