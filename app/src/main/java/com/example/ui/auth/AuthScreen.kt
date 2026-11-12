package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.LocalTypeProColors

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val ext = LocalTypeProColors.current

  LaunchedEffect(Unit) {
    GoogleAuthHelper.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      scope = scope,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
          ),
        ),
      )
      .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.size(48.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Keyboard,
              contentDescription = stringResource(R.string.cd_typepro_logo),
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(26.dp),
            )
          }
        }
        Column {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.line),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = stringResource(R.string.auth_headline),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.auth_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          FeatureHighlightRow(
            icon = Icons.Filled.Speed,
            title = "20 Lessons & 6 Exam Categories",
            subtitle = "Live WPM, grapheme-aware accuracy, SSC/Railway passages",
          )
          FeatureHighlightRow(
            icon = Icons.Filled.Translate,
            title = "English QWERTY & Hindi Devanagari",
            subtitle = "Interactive InScript layout + Roman-to-Devanagari phonetic engine",
          )
          FeatureHighlightRow(
            icon = Icons.Filled.VerifiedUser,
            title = "Cloud-Synced Reports & Streaks",
            subtitle = "Save test history, weak-key stats, game high scores & 12 badges",
          )

          if (errorMessage != null) {
            Surface(
              color = ext.dangerSoft,
              shape = MaterialTheme.shapes.small,
              border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.4f)),
            ) {
              Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = ext.danger,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          Spacer(Modifier.height(4.dp))

          Button(
            onClick = {
              errorMessage = null
              isLoading = true
              GoogleAuthHelper.onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                onAuthCancelled = {
                  isLoading = false
                },
              )
            },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("google_sign_in_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
            } else {
              Text(
                text = stringResource(R.string.sign_in_with_google),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  val ext = LocalTypeProColors.current
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      color = ext.brandSoft,
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.size(36.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }
    }
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
