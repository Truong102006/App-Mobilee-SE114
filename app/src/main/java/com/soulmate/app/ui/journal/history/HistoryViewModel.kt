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
        viewModelScope.launch {
            try {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                Log.d("HistoryViewModel", "Checking diaries for UID: $currentUid")

                diaryRepository.getDiaries(currentUid)
                    .catch { e ->
                        Log.e("HistoryViewModel", "Error fetching diaries: ${e.message}")
                    }
                    .collectLatest { diaries ->
                        Log.d("HistoryViewModel", "Received ${diaries.size} diaries from Firestore")
                        _historyNotes.clear()
                        val notes = diaries.map { diary ->
                            // Log chi tiết từng diary để kiểm tra xem content có bị null/empty không
                            Log.d("HistoryViewModel", "Diary Item -> ID: ${diary.diaryId}, Content: ${diary.content}, CreatedAt: ${diary.createdAt}")
                            
                            RecordingNote(
                                id = try { diary.diaryId.hashCode().toLong() } catch (e: Exception) { System.currentTimeMillis() },
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
                Log.e("HistoryViewModel", "Firebase error: ${e.message}")
            }
        }
    }

    fun addNote(note: RecordingNote) {
        if (!_historyNotes.any { it.text == note.text && it.dateTime == note.dateTime }) {
            _historyNotes.add(0, note)
        }
    }

    fun deleteNote(note: RecordingNote) {
        _historyNotes.remove(note)
    }

    fun updateNote(id: String, newHtml: String, newImages: List<String>, newMood: String) {
        val index = _historyNotes.indexOfFirst { it.id.toString() == id }
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

    fun getNoteById(id: String): RecordingNote? {
        return _historyNotes.find { it.id.toString() == id }
    }
}
