package com.example.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameTouchButton(
    modifier: Modifier = Modifier,
    text: String = "",
    iconContent: @Composable (() -> Unit)? = null,
    backgroundColor: Color = Color(0xFF020617), // Deep void slate
    accentColor: Color = Color.White,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp)
            .border(4.dp, if (isPressed) Color(0xFFF43F5E) else accentColor) // glowing red on hover/press
            .background(if (isPressed) accentColor.copy(alpha = 0.25f) else backgroundColor)
            .pointerInput(onDown, onUp) {
                awaitPointerEventScope {
                    while (true) {
                                                // Detect when finger lands on the button
                        awaitFirstDown()
                        isPressed = true
                        onDown()

                        // Wait until finger is raised or slide cancels
                        waitForUpOrCancellation()
                        isPressed = false
                        onUp()
                    }
                }
            }
            .padding(4.dp)
    ) {
        if (iconContent != null) {
            iconContent()
        } else {
            Text(
                text = text,
                color = if (isPressed) Color(0xFFF43F5E) else accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
