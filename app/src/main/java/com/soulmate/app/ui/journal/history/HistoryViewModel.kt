package com.soulmate.app.ui.journal.history

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.domain.repository.IDiaryRepository
import com.soulmate.app.ui.home.components.RecordingNote
import com.soulmate.app.ui.social.CommunityPost
import com.soulmate.app.ui.social.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val diaryRepository: IDiaryRepository,
    private val communityRepository: CommunityRepository
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

                diaryRepository.getDiaries(currentUid)
                    .catch { e ->
                        Log.e("HistoryViewModel", "Error fetching diaries: ${e.message}")
                    }
                    .collectLatest { diaries ->
                        _historyNotes.clear()
                        val notes = diaries.map { diary ->
                            RecordingNote(
                                id = try { diary.id.hashCode().toLong() } catch (e: Exception) { System.currentTimeMillis() },
                                text = diary.text,
                                dateTime = SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date(diary.timestamp)),
                                moodTag = diary.moodTag,
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

    fun shareDiaryToCommunity(note: RecordingNote) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        val newPost = CommunityPost(
            id = UUID.randomUUID().toString(),
            userName = currentUser?.displayName ?: "Anonymous User",
            userAvatarUrl = currentUser?.photoUrl?.toString(),
            mood = note.moodTag,
            timeAgo = "Just now",
            textContent = note.text,
            imageUrls = note.imageUrls,
            likeCount = 0,
            commentCount = 0,
            viewCount = 0
        )

        communityRepository.addPost(newPost)
    }

}
