package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.ui.components.RealityEngineDialerGrid
import com.example.ui.theme.RealityEngineTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RealityEngineDialerTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testDialerGridKeyPress() {
    var pressedDigit = ""
    composeTestRule.setContent {
      RealityEngineTheme {
        Box(modifier = Modifier.padding(16.dp)) {
          RealityEngineDialerGrid(
            onDigitClick = { digit -> pressedDigit = digit }
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("keypad_digit_5").performClick()
    assertEquals("5", pressedDigit)

    composeTestRule.onNodeWithTag("keypad_digit_0").performClick()
    assertEquals("0", pressedDigit)
  }
}

