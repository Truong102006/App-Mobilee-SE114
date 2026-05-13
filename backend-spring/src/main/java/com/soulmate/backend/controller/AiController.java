package com.soulmate.backend.controller;

import com.soulmate.backend.dto.ai.PredictMoodRequest;
import com.soulmate.backend.dto.ai.PredictMoodResponse;
import com.soulmate.backend.service.GeminiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/secure/ai")
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/predict-mood")
    public PredictMoodResponse predictMood(@Valid @RequestBody PredictMoodRequest request) {
        return geminiService.predictMood(request.text());
    }
}
