package com.example.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.sin

/**
 * A beautiful interactive 3D Tilting Modifier that reacts to touch coordinate pressure
 * and tilts the element dynamically in 3D perspective space with a smooth rebound spring.
 * It also renders a glossy reflection on top of the card when tilted to make it look truly 3D and physical!
 */
fun Modifier.tilt3D(
    maxRotationX: Float = 12f,
    maxRotationY: Float = 12f,
    scaleOnTouch: Float = 0.96f
): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var touchX by remember { mutableStateOf(0f) }
    var touchY by remember { mutableStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }

    val rotX by animateFloatAsState(
        targetValue = if (isPressed && size.height > 0) {
            val halfHeight = size.height / 2f
            -((touchY - halfHeight) / halfHeight).coerceIn(-1.2f, 1.2f) * maxRotationX
        } else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
    )

    val rotY by animateFloatAsState(
        targetValue = if (isPressed && size.width > 0) {
            val halfWidth = size.width / 2f
            ((touchX - halfWidth) / halfWidth).coerceIn(-1.2f, 1.2f) * maxRotationY
        } else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleOnTouch else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
    )

    // Gloss glow factor based on rotation
    val glossOpacity by animateFloatAsState(
        targetValue = if (isPressed) 0.15f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    )

    this
        .onSizeChanged { size = it }
        .pointerInput(size) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    touchX = down.position.x
                    touchY = down.position.y

                    do {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        if (anyPressed) {
                            val active = event.changes.firstOrNull { it.pressed }
                            if (active != null) {
                                touchX = active.position.x
                                touchY = active.position.y
                            }
                        } else {
                            isPressed = false
                        }
                    } while (isPressed)
                }
            }
        }
        .graphicsLayer {
            rotationX = rotX
            rotationY = rotY
            scaleX = scale
            scaleY = scale
            cameraDistance = 16f * density
        }
        .drawWithContent {
            drawContent()
            // Physical gloss layer
            if (glossOpacity > 0f && size.width > 0 && size.height > 0) {
                val brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = glossOpacity),
                        Color.White.copy(alpha = 0f)
                    ),
                    start = androidx.compose.ui.geometry.Offset(touchX - size.width / 3f, touchY - size.height / 3f),
                    end = androidx.compose.ui.geometry.Offset(touchX + size.width / 2f, touchY + size.height / 2f)
                )
                drawRect(brush = brush)
            }
        }
}

/**
 * A colorful shifting 3D fluid gradient that loops smoothly in the background, supplying
 * deep, rich neon/cozy ambiance to selected layouts.
 */
fun Modifier.animatedColorsBg(): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "GradientAnim")
    
    // Cycle state
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * sin(Math.PI.toFloat()), // Use math boundaries for perfect looping
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    // Smoothly morphable vibrant color elements
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFFF3E5F5), // Pastel lavender light
        targetValue = Color(0xFFE8EAF6), // Soft indigo light
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "c1"
    )

    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFFE0F7FA), // Mint/Cyan light
        targetValue = Color(0xFFE8F5E9), // Clean pastel green
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "c2"
    )

    val color3 by infiniteTransition.animateColor(
        initialValue = Color(0xFFFFF3E0), // Cozy warm peach
        targetValue = Color(0xFFFFF8E1), // Golden cream
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "c3"
    )

    val brush = remember(phase, color1, color2, color3) {
        Brush.verticalGradient(
            colors = listOf(
                color1.copy(alpha = 0.85f),
                color2.copy(alpha = 0.85f),
                color3.copy(alpha = 0.85f)
            )
        )
    }

    this.background(brush)
}
