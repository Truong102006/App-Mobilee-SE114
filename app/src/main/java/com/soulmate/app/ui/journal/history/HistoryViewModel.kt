package com.soulmate.app.ui.journal.history

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.domain.repository.IDiaryRepository
import com.soulmate.app.ui.home.components.RecordingNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val diaryRepository: IDiaryRepository
) : ViewModel() {
    private val _historyNotes = mutableStateListOf<RecordingNote>()
    val historyNotes: List<RecordingNote> = _historyNotes

    init {
        observeDiaries()
    }

    private fun observeDiaries() {
        // Xóa sạch list cũ ngay khi bắt đầu để tránh hiện dữ liệu của User trước đó
        _historyNotes.clear()

        viewModelScope.launch {
            try {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                
                if (currentUid.isNullOrEmpty()) {
                    Log.w("HistoryViewModel", "No user logged in")
                    _historyNotes.clear()
                    return@launch
                }

                Log.d("HistoryViewModel", "Observing diaries for UID: $currentUid")

                diaryRepository.getDiaries(currentUid)
                    .catch { e ->
                        Log.e("HistoryViewModel", "Error: ${e.message}")
                        _historyNotes.clear()
                    }
                    .collectLatest { diaries ->
                        Log.d("HistoryViewModel", "Received ${diaries.size} diaries for $currentUid")
                        _historyNotes.clear()
                        val notes = diaries.map { diary ->
                            RecordingNote(id = diary.diaryId.hashCode().toLong(),
                                diaryId = diary.diaryId,
                                text = if (diary.content.isEmpty()) "(Không có nội dung)" else diary.content,
                                dateTime = SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date(diary.createdAt)),
                                moodTag = diary.moodTag,
                                imageUrls = diary.imageUrls
                            )
                        }
                        _historyNotes.addAll(notes)
                    }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Critical error: ${e.message}")
                _historyNotes.clear()
            }
        }
    }

    suspend fun saveQuickNote(
        note: RecordingNote,
        currentUid: String? = FirebaseAuth.getInstance().currentUser?.uid,
        fallbackTimeMillis: Long = System.currentTimeMillis()
    ): Result<Unit> {
        val userId = currentUid?.trim().orEmpty()
        if (userId.isBlank()) {
            val error = IllegalStateException("No user logged in")
            Log.w("HistoryViewModel", error.message ?: "No user logged in")
            return Result.failure(error)
        }

        val normalizedText = note.text.trim()
        if (normalizedText.isBlank()) {
            val error = IllegalArgumentException("Nội dung nhật kí đang trống")
            Log.w("HistoryViewModel", error.message ?: "Diary content is empty")
            return Result.failure(error)
        }

        return diaryRepository.saveDiary(
            recordingNoteToDiary(
                note = note.copy(text = normalizedText),
                userId = userId,
                fallbackTimeMillis = fallbackTimeMillis
            )
        ).onFailure { error ->
            Log.e("HistoryViewModel", "Quick save failed", error)
        }
    }

    fun deleteNote(note: RecordingNote) {
        viewModelScope.launch {
            // 1. Xóa trên Firestore (dùng note.id.toString() hoặc nếu bạn lưu ID gốc thì dùng nó)
            diaryRepository.deleteDiary(note.diaryId)
                .onSuccess {
                    // 2. Nếu xóa server thành công thì mới xóa trên UI
                    _historyNotes.remove(note)
                }
                .onFailure {
                    Log.e("HistoryViewModel", "Xóa thất bại: ${it.message}")
                }
        }
    }

    fun updateNote(diaryId: String, newHtml: String, newImages: List<String>, newMood: String) {
        // So sánh trực tiếp với diaryId (String)
        val index = _historyNotes.indexOfFirst { it.diaryId == diaryId }
        if (index != -1) {
            val oldNote = _historyNotes[index]
            val updatedNote = oldNote.copy(
                text = newHtml,
                imageUrls = newImages,
                moodTag = newMood
            )
            _historyNotes[index] = updatedNote
        }
    }

    fun getNoteById(diaryId: String): RecordingNote? {
        return _historyNotes.find { it.diaryId == diaryId }
    }
}

internal fun recordingNoteToDiary(
    note: RecordingNote,
    userId: String,
    fallbackTimeMillis: Long = System.currentTimeMillis(),
    locale: Locale = Locale.getDefault()
): Diary {
    val createdAt = parseRecordingNoteDateTime(note.dateTime, locale) ?: fallbackTimeMillis
    val updatedAt = maxOf(createdAt, fallbackTimeMillis)
    val normalizedText = note.text.trim()
    val derivedTitle = normalizedText
        .lineSequence()
        .map(String::trim)
        .firstOrNull { it.isNotEmpty() }
        ?.take(80)
        .orEmpty()

    return Diary(
        diaryId = note.diaryId,
        userId = userId,
        title = derivedTitle,
        content = normalizedText,
        imageUrls = note.imageUrls,
        moodTag = note.moodTag,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

internal fun parseRecordingNoteDateTime(
    value: String,
    locale: Locale = Locale.getDefault()
): Long? {
    if (value.isBlank()) {
        return null
    }

    return runCatching {
        SimpleDateFormat("dd/MM/yyyy HH:mm", locale).apply {
            isLenient = false
        }.parse(value.trim())?.time
    }.getOrNull()
}
