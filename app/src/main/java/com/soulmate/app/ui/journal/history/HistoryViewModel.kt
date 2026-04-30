package com.soulmate.app.ui.journal.history

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmate.app.domain.repository.IDiaryRepository
import com.soulmate.app.ui.home.components.RecordingNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
                diaryRepository.getDiaries("current_user_id")
                    .catch { e -> 
                        Log.e("HistoryViewModel", "Error fetching diaries: ${e.message}")
                    }
                    .collectLatest { diaries ->
                        _historyNotes.clear()
                        val notes = diaries.map { diary ->
                            RecordingNote(
                                id = try { diary.id.hashCode().toLong() } catch (e: Exception) { System.currentTimeMillis() },
                                text = diary.text,
                                dateTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(diary.timestamp)),
                                moodTag = diary.moodTag
                            )
                        }
                        _historyNotes.addAll(notes)
                    }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Firebase initialization error: ${e.message}")
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

    fun updateNote(noteId: Long, newText: String) {
        val index = _historyNotes.indexOfFirst { it.id == noteId }
        if (index != -1) {
            val updatedNote = _historyNotes[index].copy(text = newText)
            _historyNotes[index] = updatedNote
        }
    }
}
