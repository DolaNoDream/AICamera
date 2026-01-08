package com.example.aicamerabackend.controller;

import com.example.aicamerabackend.dto.VoiceTextRequest;
import com.example.aicamerabackend.dto.VoiceTextResponse;
import com.example.aicamerabackend.service.GuidanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class VoiceController {

    private final GuidanceService guidanceService;

    @PostMapping("/api/voice/text")
    public VoiceTextResponse voiceText(@Valid @RequestBody VoiceTextRequest req) {
        //return guidanceService.handleVoiceTextMock(req.getSessionId(), req.getText());
        return guidanceService.handleVoiceText(req.getSessionId(), req.getText());

    }
}
