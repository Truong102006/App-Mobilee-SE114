package com.soulmate.backend.dto.community;

public record ResolveReportRequest(String postId, String action) {}
// "delete" or "ignore"