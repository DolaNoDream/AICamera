package com.example.aicamerabackend.client;

import com.example.aicamerabackend.dto.PoseSugResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PoseAiClient {
    private final WebClient webClient;

    @Value("${ai.pose.base-url}")
    private String baseUrl;

    public PoseSugResponse poseSug(MultipartFile image, String sessionId, String userIntent, String meta) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();

            ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            };

            builder.part("sessionId", sessionId);
            builder.part("image", imageResource).contentType(MediaType.APPLICATION_OCTET_STREAM);

            if (userIntent != null && !userIntent.isBlank()) {
                builder.part("userIntent", userIntent);
            }
            if (meta != null && !meta.isBlank()) {
                builder.part("meta", meta);
            }

            return webClient.post()
                    .uri(baseUrl + "/posesug")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(PoseSugResponse.class)
                    .timeout(Duration.ofSeconds(3))
                    .onErrorResume(e -> Mono.error(new RuntimeException("Pose AI call failed: " + e.getMessage(), e)))
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Pose AI call failed: " + e.getMessage(), e);
        }
    }
}
