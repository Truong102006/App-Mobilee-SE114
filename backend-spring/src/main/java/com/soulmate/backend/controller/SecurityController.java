package com.soulmate.backend.controller;

import com.soulmate.backend.security.AuthContext;
import com.soulmate.backend.security.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/secure")
public class SecurityController {

    @GetMapping("/ping")
    public Map<String, Object> ping(HttpServletRequest request) {
        AuthContext auth = AuthContextHolder.getRequired(request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("uid", auth.uid());
        response.put("appId", auth.appId());
        response.put("serverTime", System.currentTimeMillis());
        return response;
    }
}
