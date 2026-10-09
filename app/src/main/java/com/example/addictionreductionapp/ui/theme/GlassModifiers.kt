package com.example.addictionreductionapp.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glass glow shadow — shared modifier for Login/Register/Permission cards.
 * Draws outer glow outside bounds while clipping inner area (prevents black shadow bug).
 *
 * LAG FIX: setShadowLayer forces a SOFTWARE layer + offscreen bitmap per card.
 * Capped radius to 8dp and alpha to 0.2 so overdraw stays cheap. Callers should
 * avoid stacking this with .shadow() on the same card.
 */
fun Modifier.glassGlow(
    color: Color,
    borderRadius: Dp = 18.dp,
    glowRadius: Dp = 14.dp,
    glowAlpha: Float = 0.5f,
    offsetY: Dp = 0.dp
): Modifier = this.then(
    if (glowAlpha < 0.01f) Modifier else Modifier.drawWithCache {
        val glowColor = color.copy(alpha = glowAlpha.coerceIn(0f, 0.2f))
        val glowPx = glowRadius.toPx().coerceAtMost(8.dp.toPx())
        val borderPx = borderRadius.toPx()
        val offsetYPx = offsetY.toPx()
        val innerPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(borderPx)
                )
            )
        }
        val frameworkPaint = Paint().asFrameworkPaint().apply {
            this.color = glowColor.toArgb()
            setShadowLayer(glowPx, 0f, offsetYPx, glowColor.toArgb())
        }
        val shadowPaint = Paint().apply { asFrameworkPaint().set(frameworkPaint) }
        onDrawBehind {
            if (size.width < 2f || size.height < 2f) return@onDrawBehind
            clipPath(innerPath, clipOp = ClipOp.Difference) {
                drawIntoCanvas { canvas ->
                    canvas.drawRoundRect(
                        left = 0f, top = 0f, right = size.width, bottom = size.height,
                        radiusX = borderPx, radiusY = borderPx, paint = shadowPaint
                    )
                }
            }
        }
    }
)
