package io.github.aadi1607.habittracker.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.aadi1607.habittracker.ui.theme.HabitPalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val color: Color,
    val radius: Float,
    val drift: Float,
)

/**
 * One-shot celebratory burst that plays each time [celebrate] flips to true
 * (i.e. the moment the last habit of the day is completed). Purely visual —
 * draws above the content and never intercepts touches.
 */
@Composable
fun ConfettiOverlay(celebrate: Boolean, modifier: Modifier = Modifier) {
    var burst by remember { mutableIntStateOf(0) }
    var wasCelebrating by remember { mutableStateOf(celebrate) }
    LaunchedEffect(celebrate) {
        if (celebrate && !wasCelebrating) burst++
        wasCelebrating = celebrate
    }
    if (burst == 0) return

    val particles = remember(burst) {
        val random = Random(burst)
        List(90) {
            ConfettiParticle(
                angle = (-90f + random.nextFloat() * 120f - 60f) * (kotlin.math.PI.toFloat() / 180f),
                speed = 0.5f + random.nextFloat() * 0.9f,
                color = Color(HabitPalette[random.nextInt(HabitPalette.size)]),
                radius = 3f + random.nextFloat() * 5f,
                drift = (random.nextFloat() - 0.5f) * 0.6f,
            )
        }
    }
    val progress = remember(burst) { Animatable(0f) }
    LaunchedEffect(burst) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(1_600, easing = LinearOutSlowInEasing))
    }

    if (progress.value < 1f) {
        val t = progress.value
        Canvas(modifier = modifier.fillMaxSize()) {
            val origin = Offset(size.width / 2f, size.height * 0.75f)
            val reach = size.height * 0.9f
            particles.forEach { p ->
                val distance = p.speed * reach * t
                val x = origin.x + cos(p.angle) * distance + p.drift * size.width * t * t
                val y = origin.y + sin(p.angle) * distance + size.height * 0.55f * t * t
                drawCircle(
                    color = p.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                    radius = p.radius.dp.toPx() * (1f - 0.4f * t),
                    center = Offset(x, y),
                )
            }
        }
    }
}
