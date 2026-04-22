package com.soulmate.app.ui.journal.history

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.soulmate.app.ui.home.components.RecordingNote
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor() : ViewModel() {
    private val _historyNotes = mutableStateListOf<RecordingNote>()
    val historyNotes: List<RecordingNote> = _historyNotes

    fun addNote(note: RecordingNote) {
        _historyNotes.add(0, note)
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
