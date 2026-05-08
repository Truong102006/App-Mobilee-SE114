package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.Diary
import kotlinx.coroutines.flow.Flow

interface IDiaryRepository {
    // Lấy dòng dữ liệu nhật ký (Flow) theo userId
    fun getDiaries(userId: String): Flow<List<Diary>>

    // Lưu một bài nhật ký mới hoặc ghi đè bài cũ
    suspend fun saveDiary(diary: Diary): Result<Unit>

    // Tải danh sách tất cả nhật ký
    suspend fun loadDiaries(): Result<List<Diary>>

    // Tải chi tiết một bài nhật ký theo ID
    suspend fun loadDiaryById(diaryId: String): Result<Diary?>

    // Cập nhật một phần nội dung bài nhật ký (ví dụ: chỉ sửa title hoặc mood)
    suspend fun patchDiary(diaryId: String, updates: Map<String, Any?>): Result<Unit>

    suspend fun deleteDiary(diaryId: String): Result<Unit>
}
