package com.example.pulseledger

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.pulseledger.ui.navigation.PulseMainShell
import com.example.pulseledger.ui.screens.onboarding.OnboardingHelper
import com.example.pulseledger.ui.screens.onboarding.OnboardingScreen

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val initialKey = remember {
    if (!OnboardingHelper.isOnboardingCompleted(context) && !OnboardingHelper.isNotificationListenerGranted(context)) {
      Onboarding
    } else {
      Main
    }
  }

  val backStack = rememberNavBackStack(initialKey)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Onboarding> {
          OnboardingScreen(
            onComplete = {
              backStack.clear()
              backStack.add(Main)
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Main> {
          PulseMainShell(
            onOpenOnboarding = { backStack.add(Onboarding) },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}
