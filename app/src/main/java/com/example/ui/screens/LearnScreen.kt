package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ContentCatalog
import com.example.domain.LessonDef
import com.example.domain.ResultInput
import com.example.domain.TypingMode
import com.example.ui.TypeProAppState
import com.example.ui.components.BadgePill
import com.example.ui.components.LinearProgressMeter
import com.example.ui.components.SectionCard
import com.example.ui.components.Tone
import com.example.ui.components.TypingSurface
import com.example.ui.theme.LocalTypeProColors

@Composable
fun LearnScreen(
  state: TypeProAppState,
  selectedLessonSlug: String?,
  onSelectLesson: (String?) -> Unit,
  onSaveResult: suspend (ResultInput) -> Result<String>,
  modifier: Modifier = Modifier,
) {
  if (selectedLessonSlug != null) {
    val lesson = ContentCatalog.LESSONS.firstOrNull { it.slug == selectedLessonSlug }
    if (lesson != null) {
      LessonRunnerScreen(
        lesson = lesson,
        state = state,
        onBack = { onSelectLesson(null) },
        onSelectLesson = onSelectLesson,
        onSaveResult = onSaveResult,
        modifier = modifier,
      )
      return
    }
  }

  val ext = LocalTypeProColors.current
  val completedCount = ContentCatalog.LESSONS.count { state.lessonProgress[it.slug]?.completed == true }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SectionCard(
      title = "Structured typing curriculum",
      description = "20 progressive English lessons from home row anchors to competitive exam passages.",
      action = {
        BadgePill(text = "$completedCount/${ContentCatalog.LESSONS.size} done", tone = Tone.SUCCESS)
      },
    ) {
      LinearProgressMeter(
        percent = (completedCount.toFloat() / ContentCatalog.LESSONS.size) * 100f,
        tone = Tone.SUCCESS,
        height = 10.dp,
      )
    }

    ContentCatalog.COURSES.forEach { course ->
      val courseLessons = ContentCatalog.LESSONS.filter { it.courseSlug == course.slug }
      val courseDone = courseLessons.count { state.lessonProgress[it.slug]?.completed == true }

      SectionCard(
        title = course.title,
        description = course.description,
        action = {
          BadgePill(text = "$courseDone/${courseLessons.size}", tone = Tone.BRAND)
        },
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          courseLessons.forEach { lesson ->
            val prog = state.lessonProgress[lesson.slug]
            val isCompleted = prog?.completed == true

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { onSelectLesson(lesson.slug) }
                .testTag("lesson_item_${lesson.slug}"),
              shape = MaterialTheme.shapes.medium,
              color = ext.panel,
              border = BorderStroke(1.dp, if (isCompleted) ext.success.copy(alpha = 0.45f) else ext.line),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                  contentDescription = if (isCompleted) "Completed" else "Not completed",
                  tint = if (isCompleted) ext.success else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${lesson.sortOrder}. ${lesson.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = if (prog != null) {
                      "${lesson.focus} · Best ${prog.bestWpm.toInt()} WPM · ${prog.bestAccuracy.toInt()}% acc"
                    } else {
                      "${lesson.focus} · ${lesson.estMinutes} min"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "Open lesson",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }
}

@Composable
private fun LessonRunnerScreen(
  lesson: LessonDef,
  state: TypeProAppState,
  onBack: () -> Unit,
  onSelectLesson: (String) -> Unit,
  onSaveResult: suspend (ResultInput) -> Result<String>,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBack() }

  val idx = ContentCatalog.LESSONS.indexOfFirst { it.slug == lesson.slug }
  val nextLesson = ContentCatalog.LESSONS.getOrNull(idx + 1)

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("lesson_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to lessons")
        }
        Column {
          Text(
            text = lesson.courseTitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = lesson.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      BadgePill(text = lesson.level.label, tone = Tone.BRAND)
    }

    TypingSurface(
      target = lesson.content,
      language = lesson.language,
      durationSeconds = 0,
      mode = TypingMode.LESSON,
      refId = lesson.slug,
      refTitle = lesson.title,
      settings = state.settings,
      onSaveResult = onSaveResult,
      allowVirtualInput = true,
      restartLabel = "Retry lesson",
      instructions = lesson.instructions,
      extraResultActions = {
        if (nextLesson != null) {
          Button(
            onClick = { onSelectLesson(nextLesson.slug) },
            modifier = Modifier.testTag("next_lesson_button"),
          ) {
            Text("Next: ${nextLesson.title}")
          }
        }
        OutlinedButton(onClick = onBack) {
          Text("All lessons")
        }
      },
    )
    Spacer(Modifier.height(12.dp))
  }
}
