package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TypeProRepository
import com.example.domain.AchievementState
import com.example.domain.KeyStat
import com.example.domain.LessonProgress
import com.example.domain.ProgressSummary
import com.example.domain.ResultInput
import com.example.domain.StatsCalculator
import com.example.domain.StoredResult
import com.example.domain.UserSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataStatus {
  data object Loading : DataStatus
  data object Ready : DataStatus
  data class Error(val message: String) : DataStatus
}

data class TypeProAppState(
  val status: DataStatus = DataStatus.Loading,
  val settings: UserSettings = UserSettings(),
  val results: List<StoredResult> = emptyList(),
  val keyStats: List<KeyStat> = emptyList(),
  val lessonProgress: Map<String, LessonProgress> = emptyMap(),
  val summary: ProgressSummary = StatsCalculator.summarize(emptyList()),
  val achievements: List<AchievementState> = StatsCalculator.evaluateAchievements(
    StatsCalculator.summarize(emptyList()),
  ),
)

class TypeProViewModel(
  private val repository: TypeProRepository,
  private val currentUserId: String,
  private val initialDisplayName: String,
) : ViewModel() {

  private val _newlyUnlocked = MutableStateFlow<List<AchievementState>>(emptyList())
  val newlyUnlocked: StateFlow<List<AchievementState>> = _newlyUnlocked.asStateFlow()

  private val _notice = MutableStateFlow<String?>(null)
  val notice: StateFlow<String?> = _notice.asStateFlow()

  private var knownUnlockedKeys: Set<String>? = null

  init {
    viewModelScope.launch {
      repository.ensureProfileInitialized(initialDisplayName)
    }
  }

  val appState: StateFlow<TypeProAppState> = combine(
    repository.observeSettings(currentUserId),
    repository.observeResults(currentUserId),
    repository.observeKeyStats(currentUserId),
    repository.observeLessonProgress(currentUserId),
  ) { settings, results, keyStats, lessonProgress ->
    val summary = StatsCalculator.summarize(
      results = results,
      keyStats = keyStats,
      dailyGoalMinutes = settings.dailyGoalMinutes,
    )
    val achievements = StatsCalculator.evaluateAchievements(summary)

    val currentUnlocked = achievements.filter { it.unlocked }.map { it.key }.toSet()
    val previous = knownUnlockedKeys
    if (previous != null) {
      val newly = achievements.filter { it.unlocked && it.key !in previous }
      if (newly.isNotEmpty()) {
        _newlyUnlocked.value = newly
      }
    }
    knownUnlockedKeys = currentUnlocked

    TypeProAppState(
      status = DataStatus.Ready,
      settings = settings,
      results = results,
      keyStats = keyStats,
      lessonProgress = lessonProgress,
      summary = summary,
      achievements = achievements,
    )
  }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing TypePro user state", rootError)
      emit(
        TypeProAppState(
          status = DataStatus.Error(rootError.message ?: "Failed to sync progress"),
        ),
      )
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = TypeProAppState(),
    )

  fun updateSettings(transform: (UserSettings) -> UserSettings) {
    val updated = transform(appState.value.settings)
    viewModelScope.launch {
      val res = repository.saveSettings(updated)
      if (res.isFailure) {
        _notice.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save settings"
      }
    }
  }

  suspend fun saveSessionResult(input: ResultInput): Result<String> {
    return repository.saveResult(input)
  }

  fun clearHistory() {
    viewModelScope.launch {
      val res = repository.clearAllHistory()
      _notice.value = if (res.isSuccess) {
        "Typing history cleared."
      } else {
        res.exceptionOrNull()?.localizedMessage ?: "Unable to clear history."
      }
    }
  }

  fun dismissNewAchievements() {
    _newlyUnlocked.value = emptyList()
  }

  fun clearNotice() {
    _notice.value = null
  }

  private companion object {
    const val TAG = "TypeProVM"
  }
}
