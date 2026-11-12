package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.KeyStat
import com.example.domain.LessonProgress
import com.example.domain.ResultInput
import com.example.domain.StoredResult
import com.example.domain.TypingMode
import com.example.domain.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.max

class TypeProRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  fun observeSettings(userId: String): Flow<UserSettings> = flow {
    val docRef = userDoc(userId)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          val doc = snapshot.toObject(
            UserProfileSettingsDoc::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          doc?.toDomain() ?: UserSettings()
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        },
    )
  }

  suspend fun ensureProfileInitialized(displayName: String): Result<UserSettings> {
    val uid = requireUserId()
    val docRef = userDoc(uid)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        val existing = snapshot.toObject(
          UserProfileSettingsDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        Result.success(existing?.toDomain() ?: UserSettings())
      } else {
        val cleanName = displayName.trim().take(80).ifBlank { "Typist" }
        val defaults = UserSettings(displayName = cleanName)
        val payload = mapOf(
          "userId" to uid,
          "displayName" to defaults.displayName,
          "theme" to defaults.theme.value,
          "language" to defaults.language.code,
          "soundEnabled" to defaults.soundEnabled,
          "cursorStyle" to defaults.cursorStyle.value,
          "keyboardLayout" to defaults.keyboardLayout.value,
          "defaultDuration" to defaults.defaultDuration,
          "defaultCategory" to defaults.defaultCategory,
          "dailyGoalMinutes" to defaults.dailyGoalMinutes,
          "showKeyboard" to defaults.showKeyboard,
          "strictMode" to defaults.strictMode,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        Result.success(defaults)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveSettings(settings: UserSettings): Result<Unit> {
    val uid = requireUserId()
    val docRef = userDoc(uid)
    val cleanName = settings.displayName.trim().take(80).ifBlank { "Typist" }
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        val updateMap = mapOf(
          "displayName" to cleanName,
          "theme" to settings.theme.value,
          "language" to settings.language.code,
          "soundEnabled" to settings.soundEnabled,
          "cursorStyle" to settings.cursorStyle.value,
          "keyboardLayout" to settings.keyboardLayout.value,
          "defaultDuration" to settings.defaultDuration,
          "defaultCategory" to settings.defaultCategory,
          "dailyGoalMinutes" to settings.dailyGoalMinutes.coerceIn(5, 120),
          "showKeyboard" to settings.showKeyboard,
          "strictMode" to settings.strictMode,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updateMap).await()
      } else {
        val createMap = mapOf(
          "userId" to uid,
          "displayName" to cleanName,
          "theme" to settings.theme.value,
          "language" to settings.language.code,
          "soundEnabled" to settings.soundEnabled,
          "cursorStyle" to settings.cursorStyle.value,
          "keyboardLayout" to settings.keyboardLayout.value,
          "defaultDuration" to settings.defaultDuration,
          "defaultCategory" to settings.defaultCategory,
          "dailyGoalMinutes" to settings.dailyGoalMinutes.coerceIn(5, 120),
          "showKeyboard" to settings.showKeyboard,
          "strictMode" to settings.strictMode,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(createMap).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeResults(userId: String): Flow<List<StoredResult>> = flow {
    val colRef = userDoc(userId).collection("results")
    emitAll(
      colRef
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(200)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            TypingResultDoc::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ).map { it.toDomain() }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
          }
          throw error
        },
    )
  }

  fun observeKeyStats(userId: String): Flow<List<KeyStat>> = flow {
    val colRef = userDoc(userId).collection("keyStats")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            KeyStatDoc::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ).map { it.toDomain() }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
          }
          throw error
        },
    )
  }

  fun observeLessonProgress(userId: String): Flow<Map<String, LessonProgress>> = flow {
    val colRef = userDoc(userId).collection("lessonProgress")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            LessonProgressDoc::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ).associate { doc ->
            val domain = doc.toDomain()
            domain.lessonSlug to domain
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
          }
          throw error
        },
    )
  }

  suspend fun saveResult(input: ResultInput): Result<String> {
    val uid = requireUserId()
    val resultId = "res_${UUID.randomUUID().toString().replace("-", "")}"
    val resultDocRef = userDoc(uid).collection("results").document(resultId)
    val payload = mapOf(
      "userId" to uid,
      "mode" to input.mode.value,
      "refId" to input.refId.take(120),
      "refTitle" to input.refTitle.take(120).ifBlank { input.mode.label },
      "language" to input.language.code,
      "durationSeconds" to input.durationSeconds.coerceIn(0, 3600),
      "elapsedSeconds" to input.elapsedSeconds.coerceIn(0, 7200),
      "grossWpm" to input.grossWpm.coerceIn(0.0, 500.0),
      "netWpm" to input.netWpm.coerceIn(0.0, 500.0),
      "standardWpm" to input.standardWpm.coerceIn(0.0, 500.0),
      "accuracy" to input.accuracy.coerceIn(0.0, 100.0),
      "errors" to input.errors.coerceIn(0, 100_000),
      "totalTyped" to input.totalTyped.coerceIn(0, 100_000),
      "correctChars" to input.correctChars.coerceIn(0, 100_000),
      "keystrokeErrors" to input.keystrokeErrors.coerceIn(0, 100_000),
      "backspaceCount" to input.backspaceCount.coerceIn(0, 100_000),
      "score" to input.score.coerceIn(0, 1_000_000),
      "createdAt" to FieldValue.serverTimestamp(),
    )
    return try {
      resultDocRef.set(payload).await()

      if (input.mode == TypingMode.LESSON && input.refId.isNotBlank() && isValidSlug(input.refId)) {
        updateLessonProgress(uid, input.refId, input.netWpm, input.accuracy)
      }

      if (input.keyErrors.isNotEmpty() || input.correctKeys.isNotEmpty()) {
        updateKeyStats(uid, input.correctKeys, input.keyErrors.map { it.expected })
      }

      Result.success(resultId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, resultDocRef.path)
      Result.failure(e)
    }
  }

  private suspend fun updateLessonProgress(
    uid: String,
    lessonSlug: String,
    wpm: Double,
    accuracy: Double,
  ) {
    val docRef = userDoc(uid).collection("lessonProgress").document(lessonSlug)
    try {
      val existingSnap = docRef.get().await()
      if (existingSnap.exists()) {
        val existingDoc = existingSnap.toObject(LessonProgressDoc::class.java)
        val bestWpm = max(existingDoc?.bestWpm ?: 0.0, wpm).coerceIn(0.0, 500.0)
        val bestAcc = max(existingDoc?.bestAccuracy ?: 0.0, accuracy).coerceIn(0.0, 100.0)
        val attempts = ((existingDoc?.attempts ?: 0L) + 1L).coerceIn(1L, 100_000L)
        docRef.update(
          mapOf(
            "completed" to true,
            "bestWpm" to bestWpm,
            "bestAccuracy" to bestAcc,
            "attempts" to attempts,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "lessonSlug" to lessonSlug,
            "completed" to true,
            "bestWpm" to wpm.coerceIn(0.0, 500.0),
            "bestAccuracy" to accuracy.coerceIn(0.0, 100.0),
            "attempts" to 1L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
    }
  }

  private suspend fun updateKeyStats(
    uid: String,
    correctKeys: Map<String, Int>,
    incorrectExpectedKeys: List<String>,
  ) {
    val incorrectCounts = mutableMapOf<String, Int>()
    for (k in incorrectExpectedKeys) {
      if (k.isNotBlank() && k != " ") {
        val key = k.take(16)
        incorrectCounts[key] = (incorrectCounts[key] ?: 0) + 1
      }
    }

    val allKeys = (correctKeys.keys + incorrectCounts.keys)
      .filter { it.isNotBlank() && it != " " }
      .take(40)

    for (keyChar in allKeys) {
      val docId = encodeKeyId(keyChar)
      val docRef = userDoc(uid).collection("keyStats").document(docId)
      val addCorrect = (correctKeys[keyChar] ?: 0).coerceAtLeast(0).toLong()
      val addIncorrect = (incorrectCounts[keyChar] ?: 0).coerceAtLeast(0).toLong()
      if (addCorrect == 0L && addIncorrect == 0L) continue

      try {
        val snap = docRef.get().await()
        if (snap.exists()) {
          val curCorrect = snap.getLong("correct") ?: 0L
          val curIncorrect = snap.getLong("incorrect") ?: 0L
          docRef.update(
            mapOf(
              "correct" to (curCorrect + addCorrect).coerceIn(0L, 10_000_000L),
              "incorrect" to (curIncorrect + addIncorrect).coerceIn(0L, 10_000_000L),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          ).await()
        } else {
          docRef.set(
            mapOf(
              "userId" to uid,
              "keyChar" to keyChar.take(16),
              "correct" to addCorrect.coerceIn(0L, 10_000_000L),
              "incorrect" to addIncorrect.coerceIn(0L, 10_000_000L),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          ).await()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
      }
    }
  }

  suspend fun clearAllHistory(): Result<Unit> {
    val uid = requireUserId()
    val userRef = userDoc(uid)
    return try {
      for (sub in listOf("results", "keyStats", "lessonProgress")) {
        val snap = userRef.collection(sub).get().await()
        val chunks = snap.documents.chunked(400)
        for (chunk in chunks) {
          val batch = db.batch()
          chunk.forEach { batch.delete(it.reference) }
          batch.commit().await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, userRef.path)
      Result.failure(e)
    }
  }

  companion object {
    private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")

    fun isValidSlug(slug: String): Boolean = slug.length in 1..128 && VALID_ID_REGEX.matches(slug)

    fun encodeKeyId(keyChar: String): String {
      val hex = keyChar.codePoints().toArray().joinToString("_") { cp ->
        cp.toString(16)
      }
      return "k_$hex".take(120)
    }
  }
}
