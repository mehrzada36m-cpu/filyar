package com.filyar.app.ui

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A glossy glass folder: variant 0 is deep blue, variant 1 is light cyan. */
@Composable
fun GlassFolderIcon(size: Dp, variant: Int, modifier: Modifier = Modifier) {
    val top = if (variant == 0) Color(0xFF6BB6FF) else Color(0xFFC8F7FB)
    val bottom = if (variant == 0) Color(0xFF2F6FDB) else Color(0xFF3DD3EA)
    val edge = if (variant == 0) Color(0xFF1E48B8) else Color(0xFF6FE0F0)
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)
        val r = 0.07f
        val body = Path().apply {
            moveTo((0.06f + r) * w, 0.16f * h)
            lineTo(0.37f * w, 0.16f * h)
            cubicTo(0.43f * w, 0.16f * h, 0.45f * w, 0.27f * h, 0.52f * w, 0.27f * h)
            lineTo((0.94f - r) * w, 0.27f * h)
            quadraticBezierTo(0.94f * w, 0.27f * h, 0.94f * w, (0.27f + r) * h)
            lineTo(0.94f * w, (0.88f - r) * h)
            quadraticBezierTo(0.94f * w, 0.88f * h, (0.94f - r) * w, 0.88f * h)
            lineTo((0.06f + r) * w, 0.88f * h)
            quadraticBezierTo(0.06f * w, 0.88f * h, 0.06f * w, (0.88f - r) * h)
            lineTo(0.06f * w, (0.16f + r) * h)
            quadraticBezierTo(0.06f * w, 0.16f * h, (0.06f + r) * w, 0.16f * h)
            close()
        }
        // soft reflection under the folder
        drawOval(bottom.copy(alpha = 0.14f), topLeft = p(0.12f, 0.90f), size = Size(0.76f * w, 0.07f * h))
        drawPath(body, Brush.linearGradient(listOf(top, bottom), start = p(0.2f, 0.1f), end = p(0.8f, 0.95f)))
        clipPath(body) {
            val sheen = Path().apply {
                moveTo(0.06f * w, 0.88f * h)
                lineTo(0.50f * w, 0.88f * h)
                lineTo(0.94f * w, 0.40f * h)
                lineTo(0.94f * w, 0.27f * h)
                lineTo(0.70f * w, 0.27f * h)
                lineTo(0.06f * w, 0.80f * h)
                close()
            }
            drawPath(sheen, Color.White.copy(alpha = 0.18f))
            drawPath(body, Color.White.copy(alpha = 0.35f), style = Stroke(width = 0.05f * w))
        }
        drawPath(body, edge, style = Stroke(width = 0.028f * w))
    }
}

/**
 * Backdrop of the home screen: the chosen picture, or (in colour mode) the app colour with soft colour
 * glows. [blurPct] (0–100) sets how blurred it is; blurring needs Android 12 or newer.
 */
@Composable
fun HomeBackdrop(image: ImageBitmap?, blurPct: Int, glass: Boolean, modifier: Modifier = Modifier, dense: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val canBlur = Build.VERSION.SDK_INT >= 31
    Box(modifier.fillMaxSize().clipToBounds()) {
        if (image != null) {
            val blurred = canBlur && blurPct > 0
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = if (blurred) 1.15f else 1f
                        scaleX = s
                        scaleY = s
                    }
                    .then(if (blurred) Modifier.blur((blurPct * 0.32f).dp, BlurredEdgeTreatment.Unbounded) else Modifier),
            )
            Box(Modifier.fillMaxSize().background(scheme.background.copy(alpha = if (dense) 0.55f else if (glass) 0.25f else 0.50f)))
        } else {
            val base = if (glass) {
                Brush.verticalGradient(
                    if (dark) listOf(Color(0xFF1B1F24), Color(0xFF0E1013)) else listOf(Color(0xFFF8FAFC), Color(0xFFE3E7ED)),
                )
            } else {
                Brush.verticalGradient(listOf(scheme.background, scheme.background))
            }
            Box(Modifier.fillMaxSize().background(base))
            // Soft colour glows drawn as radial gradients (no blur layer, so no seams); the slider widens them.
            Canvas(Modifier.fillMaxSize()) {
                val a = if (dark) 0.34f else 0.30f
                val m = this.size.minDimension
                val k = 0.40f + 0.35f * blurPct / 100f
                fun glow(c: Color, cx: Float, cy: Float, rf: Float) {
                    val center = Offset(this.size.width * cx, this.size.height * cy)
                    val radius = m * rf * k * 2f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), center = center, radius = radius),
                        radius = radius,
                        center = center,
                    )
                }
                glow(scheme.primary, 0.85f, 0.12f, 0.45f)
                glow(scheme.tertiary, 0.10f, 0.45f, 0.40f)
                glow(scheme.secondary, 0.80f, 0.88f, 0.42f)
            }
        }
    }
}
