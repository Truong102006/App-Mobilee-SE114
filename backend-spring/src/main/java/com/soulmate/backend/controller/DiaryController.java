package com.soulmate.backend.controller;

import com.soulmate.backend.dto.diary.DeleteDiaryResponse;
import com.soulmate.backend.dto.diary.ListDiariesResponse;
import com.soulmate.backend.dto.diary.SaveDiaryRequest;
import com.soulmate.backend.dto.diary.SaveDiaryResponse;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.service.DiaryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secure/diaries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @PostMapping("/save")
    public SaveDiaryResponse saveDiary(
        HttpServletRequest request,
        @Valid @RequestBody SaveDiaryRequest body
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return diaryService.saveDiary(uid, body);
    }

    @GetMapping("/me")
    public ListDiariesResponse listMyDiaries(HttpServletRequest request) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return diaryService.listMyDiaries(uid);
    }

    @DeleteMapping("/{diaryId}")
    public DeleteDiaryResponse deleteDiary(
        HttpServletRequest request,
        @PathVariable String diaryId
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return diaryService.deleteDiary(uid, diaryId);
    }
}
