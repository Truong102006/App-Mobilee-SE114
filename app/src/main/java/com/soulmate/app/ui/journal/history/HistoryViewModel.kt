package com.soulmate.app.ui.journal.history

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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

    fun addNote(note: RecordingNote) {
        if (!_historyNotes.any { it.text == note.text && it.dateTime == note.dateTime }) {
            _historyNotes.add(0, note)
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
