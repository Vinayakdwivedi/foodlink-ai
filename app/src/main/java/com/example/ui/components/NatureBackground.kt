package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.LightGreenBg
import com.example.ui.theme.OliveDark
import com.example.ui.theme.OliveMain
import com.example.ui.theme.SoftGold

@Composable
fun NatureBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val bgGradient = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF10160B),
                Color(0xFF1A2412),
                Color(0xFF0F140A)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                LightGreenBg,
                Color(0xFFE2EFE4),
                LightGreenBg
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        // Draw elegant organic vector curves (representing stylized leaves and natural flows)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val strokeColor = if (isDarkTheme) {
                OliveMain.copy(alpha = 0.12f)
            } else {
                OliveMain.copy(alpha = 0.08f)
            }

            val goldColor = SoftGold.copy(alpha = if (isDarkTheme) 0.08f else 0.05f)

            // Flow line 1
            val path1 = Path().apply {
                moveTo(0f, height * 0.2f)
                cubicTo(
                    width * 0.3f, height * 0.1f,
                    width * 0.6f, height * 0.4f,
                    width, height * 0.3f
                )
            }
            drawPath(path = path1, color = strokeColor, style = Stroke(width = 4f))

            // Flow line 2 (gold accent)
            val path2 = Path().apply {
                moveTo(0f, height * 0.7f)
                cubicTo(
                    width * 0.4f, height * 0.85f,
                    width * 0.7f, height * 0.55f,
                    width, height * 0.75f
                )
            }
            drawPath(path = path2, color = goldColor, style = Stroke(width = 3f))

            // Draw stylized leaf shape in bottom right
            val leafPath = Path().apply {
                moveTo(width * 0.8f, height)
                quadraticTo(width * 0.9f, height * 0.85f, width, height * 0.85f)
                quadraticTo(width * 0.95f, height * 0.95f, width, height)
            }
            drawPath(path = leafPath, color = strokeColor, style = Stroke(width = 2f))
            
            // Draw stylized leaf shape in top left
            val leafPath2 = Path().apply {
                moveTo(0f, height * 0.15f)
                quadraticTo(width * 0.1f, height * 0.1f, width * 0.12f, 0f)
                quadraticTo(width * 0.05f, height * 0.05f, 0f, 0f)
            }
            drawPath(path = leafPath2, color = strokeColor, style = Stroke(width = 2f))
        }

        content()
    }
}
