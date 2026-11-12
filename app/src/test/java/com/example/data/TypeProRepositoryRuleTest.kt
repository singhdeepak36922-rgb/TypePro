package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.KeyErrorPair
import com.example.domain.Language
import com.example.domain.ResultInput
import com.example.domain.ThemeMode
import com.example.domain.TypingMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TypeProRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureProfileAndUpdateSettings_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TypeProRepository(firestore, auth)

    val initRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureProfileInitialized("Alice") }
    assertTrue(initRes.isSuccess)

    val updated = initRes.getOrThrow().copy(
      theme = ThemeMode.LIGHT,
      dailyGoalMinutes = 30,
      strictMode = true,
    )
    val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveSettings(updated) }
    assertTrue(saveRes.isSuccess)

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeSettings(uid).first { it.dailyGoalMinutes == 30 && it.strictMode }
    }
    assertEquals(ThemeMode.LIGHT, observed.theme)
  }

  @Test
  fun saveResult_authenticatedOwner_persistsResultKeyStatsAndLessonProgress() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TypeProRepository(firestore, auth)

    val input = ResultInput(
      mode = TypingMode.LESSON,
      refId = "home-row-keys",
      refTitle = "Home Row Keys",
      language = Language.EN,
      durationSeconds = 0,
      elapsedSeconds = 45,
      grossWpm = 42.0,
      netWpm = 40.0,
      standardWpm = 40.5,
      accuracy = 97.5,
      errors = 1,
      totalTyped = 40,
      correctChars = 39,
      keystrokeErrors = 2,
      backspaceCount = 1,
      score = 39,
      keyErrors = listOf(KeyErrorPair(expected = "f", actual = "g")),
      correctKeys = mapOf("a" to 6, "s" to 5),
    )

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveResult(input) }
    assertTrue(saveResult.isSuccess)
    val resultId = saveResult.getOrThrow()

    val results = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeResults(uid).first { list -> list.any { it.id == resultId } }
    }
    assertTrue(results.any { it.id == resultId && it.netWpm == 40.0 })

    val lessonMap = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeLessonProgress(uid).first { map -> map.containsKey("home-row-keys") }
    }
    assertEquals(true, lessonMap["home-row-keys"]?.completed)

    val keyStats = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeKeyStats(uid).first { list -> list.any { it.key == "f" } }
    }
    assertTrue(keyStats.any { it.key == "f" && it.incorrect >= 1 })
  }

  @Test
  fun crossUserRead_isRejectedWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = TypeProRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureProfileInitialized("Alice").getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).collection("results").get().await()
      }
      fail("Expected PERMISSION_DENIED when Bob reads Alice's results")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  @Test
  fun observeResults_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repo = TypeProRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeResults(aliceUid).first() }
      fail("Expected PERMISSION_DENIED when unauthenticated")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_typepro@test.com"
    const val BOB_EMAIL = "bob_typepro@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
