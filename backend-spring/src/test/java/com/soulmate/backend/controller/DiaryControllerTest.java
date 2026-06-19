package com.soulmate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.soulmate.backend.dto.diary.DiaryItemResponse;
import com.soulmate.backend.dto.diary.ListDiariesResponse;
import com.soulmate.backend.dto.diary.SaveDiaryRequest;
import com.soulmate.backend.dto.diary.SaveDiaryResponse;
import com.soulmate.backend.security.AuthContext;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.security.FirebaseSecurityFilter;
import com.soulmate.backend.service.DiaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiaryService diaryService;

    @MockBean
    private FirebaseSecurityFilter firebaseSecurityFilter;

    @MockBean
    private FirebaseApp firebaseApp;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private Firestore firestore;

    @BeforeEach
    public void setUp() throws Exception {
        // Configure the mocked security filter to delegate to the filter chain
        // and set the mock security context.
        Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest request = invocation.getArgument(0);
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);

            String authHeader = request.getHeader("Authorization");
            String uid = "test-user-123";
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                uid = authHeader.substring(7).trim();
            }

            AuthContextHolder.set(request, new AuthContext(uid, "mock-app-id", "user"));
            chain.doFilter(request, response);
            return null;
        }).when(firebaseSecurityFilter).doFilter(any(), any(), any());
    }

    @Test
    public void testListMyDiaries_Success() throws Exception {
        String testUid = "test-user-123";
        DiaryItemResponse item = new DiaryItemResponse(
            "diary-id-abc",
            testUid,
            "Hôm nay rất vui",
            "Mọi chuyện diễn ra suôn sẻ.",
            "Happy",
            List.of("http://image.url"),
            null,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );
        ListDiariesResponse mockResponse = new ListDiariesResponse(List.of(item));

        Mockito.when(diaryService.listMyDiaries(eq(testUid))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/secure/diaries/me")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaries[0].diaryId").value("diary-id-abc"))
                .andExpect(jsonPath("$.diaries[0].title").value("Hôm nay rất vui"));
    }

    @Test
    public void testSaveDiary_Success() throws Exception {
        String testUid = "test-user-123";
        SaveDiaryRequest request = new SaveDiaryRequest(
            null,
            "Nhật ký mới",
            "Nội dung viết tay tuyệt vời",
            "Peaceful",
            List.of(),
            null
        );
        SaveDiaryResponse mockResponse = new SaveDiaryResponse("new-diary-id-xyz", System.currentTimeMillis());

        Mockito.when(diaryService.saveDiary(eq(testUid), any(SaveDiaryRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/secure/diaries/save")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaryId").value("new-diary-id-xyz"));
    }
}
