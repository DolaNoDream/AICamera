package com.example.aicamerabackend.client;

import com.example.aicamerabackend.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class VoiceAiClient {
    private final WebClient webClient;

    @Value("${ai.voice.base-url}")
    private String baseUrl;

    public String tts(String sessionId, String text) {
        // text 为空就不调 TTS，直接返回 null（避免子后端报错）
        if (text == null || text.isBlank()) return null;

        TtsRequest req = new TtsRequest(sessionId, text);

        TtsResponse resp = webClient.post()
                .uri(baseUrl + "/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(TtsResponse.class)
                .timeout(Duration.ofSeconds(2))
                .onErrorResume(e -> Mono.error(new RuntimeException("Voice TTS call failed: " + e.getMessage(), e)))
                .block();

        return resp == null ? null : resp.getAudioUrl();
    }
    public ChatResponse chat(String sessionId, String text) {
        ChatRequest req = new ChatRequest(sessionId, text);

        return webClient.post()
                .uri(baseUrl + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .timeout(Duration.ofSeconds(2))
                .onErrorResume(e -> Mono.error(new RuntimeException("Voice chat call failed: " + e.getMessage(), e)))
                .block();
    }

}
