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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A frosted glass folder: a tinted back plate, the newest file peeking out as a white card, and a translucent
 * front panel with a bright rim. Variant 0 is blue, variant 1 is cyan.
 */
@Composable
fun GlassFolderIcon(
    size: Dp,
    variant: Int,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    peek: (@Composable () -> Unit)? = null,
    panel: (@Composable () -> Unit)? = null,
) {
    // A colour chosen for this folder wins over the blue/cyan pair.
    val deep = tint ?: if (variant == 0) Color(0xFF5B7CF0) else Color(0xFF27BFD6)
    val light = if (tint != null) lerp(tint, Color.White, 0.42f) else if (variant == 0) Color(0xFF93AAFF) else Color(0xFF86E6F1)
    Box(modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val r = 0.07f
            val plate = Path().apply {
                moveTo((0.06f + r) * w, 0.12f * h)
                lineTo(0.37f * w, 0.12f * h)
                cubicTo(0.43f * w, 0.12f * h, 0.45f * w, 0.23f * h, 0.52f * w, 0.23f * h)
                lineTo((0.94f - r) * w, 0.23f * h)
                quadraticBezierTo(0.94f * w, 0.23f * h, 0.94f * w, (0.23f + r) * h)
                lineTo(0.94f * w, (0.88f - r) * h)
                quadraticBezierTo(0.94f * w, 0.88f * h, (0.94f - r) * w, 0.88f * h)
                lineTo((0.06f + r) * w, 0.88f * h)
                quadraticBezierTo(0.06f * w, 0.88f * h, 0.06f * w, (0.88f - r) * h)
                lineTo(0.06f * w, (0.12f + r) * h)
                quadraticBezierTo(0.06f * w, 0.12f * h, (0.06f + r) * w, 0.12f * h)
                close()
            }
            drawOval(deep.copy(alpha = 0.14f), topLeft = Offset(0.12f * w, 0.90f * h), size = Size(0.76f * w, 0.07f * h))
            drawPath(plate, Brush.linearGradient(listOf(light, deep), start = Offset(0.2f * w, 0.1f * h), end = Offset(0.8f * w, 0.95f * h)))
        }
        if (peek != null) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = size * 0.17f)
                    .size(size * 0.62f)
                    .clip(RoundedCornerShape(size * 0.07f))
                    .background(Color.White),
            ) { peek() }
        }
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val topLeft = Offset(0.06f * w, 0.50f * h)
            val panel = Size(0.88f * w, 0.38f * h)
            val corner = CornerRadius(0.10f * w)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(light.copy(alpha = 0.80f), deep.copy(alpha = 0.74f)), startY = 0.5f * h, endY = 0.88f * h),
                topLeft = topLeft,
                size = panel,
                cornerRadius = corner,
            )
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.05f)), startY = 0.5f * h, endY = 0.7f * h),
                topLeft = topLeft,
                size = panel,
                cornerRadius = corner,
                style = Stroke(width = 0.018f * w),
            )
        }
        // An icon sits low, on the front panel (the folder part), not up in the card.
        if (panel != null) Box(Modifier.align(Alignment.Center).offset(y = size * 0.19f)) { panel() }
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
                    if (dark) listOf(Color(0xFF050505), Color(0xFF000000)) else listOf(Color(0xFFF8FAFC), Color(0xFFE3E7ED)),
                )
            } else {
                Brush.verticalGradient(listOf(scheme.background, scheme.background))
            }
            Box(Modifier.fillMaxSize().background(base))
            // Soft colour glows drawn as radial gradients (no blur layer, so no seams); the slider widens them.
            Canvas(Modifier.fillMaxSize()) {
                val a = if (dark) 0.45f else 0.30f
                val m = this.size.minDimension
                val k = (0.40f + 0.35f * blurPct / 100f) * (if (dark) 1.25f else 1f)
                // The glow fades to the same colour at zero opacity, and is painted over the whole canvas,
                // so it has no visible edge.
                fun glow(c: Color, cx: Float, cy: Float, rf: Float) {
                    val center = Offset(this.size.width * cx, this.size.height * cy)
                    drawRect(
                        brush = Brush.radialGradient(
                            listOf(c.copy(alpha = a), c.copy(alpha = 0f)),
                            center = center,
                            radius = m * rf * k * 2f,
                        ),
                    )
                }
                // Dark mode uses fixed vivid colours: the dark theme's own colours are too dull to light up the glass.
                glow(if (dark) Color(0xFFA67C52) else scheme.primary, 0.85f, 0.12f, 0.45f)
                glow(if (dark) Color(0xFF9B5F78) else scheme.tertiary, 0.10f, 0.45f, 0.40f)
                glow(if (dark) Color(0xFF4F8A82) else scheme.secondary, 0.80f, 0.88f, 0.42f)
            }
        }
    }
}
