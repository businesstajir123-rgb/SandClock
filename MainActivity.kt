package com.example.hourglassclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F12)
                ) {
                    HourglassClockScreen()
                }
            }
        }
    }
}

@Composable
fun HourglassClockScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            HourglassWithClock(modifier = Modifier.size(300.dp))
        }
    }
}

@Composable
fun HourglassWithClock(modifier: Modifier = Modifier) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss:SSS") }
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = LocalTime.now()
            delay(16)
        }
    }

    val millisOfMinute = now.second * 1000 + now.nano / 1_000_000
    val progress = millisOfMinute.toFloat() / 60000f

    var lastMinuteTick by remember { mutableStateOf(now.minute) }
    var flipTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(now) {
        if (now.minute != lastMinuteTick) {
            flipTrigger = !flipTrigger
            lastMinuteTick = now.minute
        }
    }

    val rotationDegrees = remember { Animatable(0f) }
    LaunchedEffect(flipTrigger) {
        val target = rotationDegrees.value + 180f
        rotationDegrees.animateTo(
            target,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDegrees.value % 360f }
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            HourglassCanvas(progress = progress)
        }

        androidx.compose.material3.Text(
            text = now.format(timeFormatter),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HourglassCanvas(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val glassStroke = Color(0xFFBFCFF8).copy(alpha = 0.7f)
        val sandColor = Color(0xFFDAA520)
        val glassFill = Color(0xFF0F1724).copy(alpha = 0.15f)

        val neckWidth = w * 0.08f
        val bulbWidth = w * 0.6f
        val bulbHeight = h * 0.42f
        val centerX = w / 2f
        val centerY = h / 2f

        val bulbTopCenter = Offset(centerX, centerY - bulbHeight * 0.6f)
        val bulbBottomCenter = Offset(centerX, centerY + bulbHeight * 0.6f)

        val path = Path().apply {
            moveTo(centerX - bulbWidth / 2, bulbTopCenter.y)
            cubicTo(
                centerX - bulbWidth / 2, bulbTopCenter.y - bulbHeight * 0.2f,
                centerX - neckWidth * 1.2f, centerY - bulbHeight * 0.1f,
                centerX - neckWidth / 2, centerY
            )
            cubicTo(
                centerX - neckWidth / 2, centerY + bulbHeight * 0.1f,
                centerX - bulbWidth / 2, bulbBottomCenter.y + bulbHeight * 0.2f,
                centerX - bulbWidth / 2, bulbBottomCenter.y
            )
            lineTo(centerX + bulbWidth / 2, bulbBottomCenter.y)
            cubicTo(
                centerX + bulbWidth / 2, bulbBottomCenter.y + bulbHeight * 0.2f,
                centerX + neckWidth / 2, centerY + bulbHeight * 0.1f,
                centerX + neckWidth / 2, centerY
            )
            cubicTo(
                centerX + neckWidth * 1.2f, centerY - bulbHeight * 0.1f,
                centerX + bulbWidth / 2, bulbTopCenter.y - bulbHeight * 0.2f,
                centerX + bulbWidth / 2, bulbTopCenter.y
            )
            close()
        }

        drawPath(path = path, brush = SolidColor(glassFill))
        drawPath(path = path, color = glassStroke, style = Stroke(width = w * 0.012f))

        drawRoundRect(
            color = glassStroke,
            topLeft = Offset(centerX - neckWidth / 2, centerY - bulbHeight * 0.05f),
            size = Size(neckWidth, bulbHeight * 0.1f),
            cornerRadius = CornerRadius(neckWidth / 2, neckWidth / 2),
            alpha = 0.9f
        )

        val topSandFraction = (1f - progress).coerceIn(0f, 1f)
        val bottomSandFraction = progress.coerceIn(0f, 1f)

        val topBox = Rect(
            left = centerX - bulbWidth / 2 + (w * 0.03f),
            top = bulbTopCenter.y - bulbHeight * 0.2f,
            right = centerX + bulbWidth / 2 - (w * 0.03f),
            bottom = centerY - (neckWidth * 0.6f)
        )

        save()
        clipRect(left = topBox.left, top = topBox.top, right = topBox.right, bottom = topBox.bottom)
        val topHeight = topBox.height * topSandFraction
        drawRoundRect(
            color = sandColor,
            topLeft = Offset(topBox.left, topBox.bottom - topHeight),
            size = Size(topBox.width, topHeight + 1f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        restore()

        val streamHeight = h * 0.06f
        val streamWidth = neckWidth * 0.35f
        val streamTop = centerY - streamHeight * 0.2f
        val streamBottom = centerY + streamHeight * 0.8f
        drawLine(
            color = sandColor,
            start = Offset(centerX, streamTop),
            end = Offset(centerX, streamBottom),
            strokeWidth = streamWidth,
            cap = StrokeCap.Round
        )

        val t = System.currentTimeMillis() % 1000L / 1000f
        for (i in 0..6) {
            val phase = (i * 0.17f + t) % 1f
            val y = lerp(streamTop, streamBottom, phase)
            val xOff = sin((phase + i) * 7.2f) * (neckWidth * 0.7f)
            drawCircle(
                color = sandColor,
                radius = streamWidth * 0.45f * (0.6f + 0.4f * (1f - phase)),
                center = Offset(centerX + xOff * 0.5f, y)
            )
        }

        val bottomBox = Rect(
            left = centerX - bulbWidth / 2 + (w * 0.03f),
            top = centerY + (neckWidth * 0.6f),
            right = centerX + bulbWidth / 2 - (w * 0.03f),
            bottom = bulbBottomCenter.y + bulbHeight * 0.15f
        )

        val maxPileHeight = bottomBox.height * 0.9f
        val pileHeight = bottomSandFraction * maxPileHeight

        val pilePath = Path().apply {
            moveTo(centerX, bottomBox.bottom - pileHeight)
            lineTo(bottomBox.left + (bottomBox.width * 0.08f), bottomBox.bottom)
            lineTo(bottomBox.right - (bottomBox.width * 0.08f), bottomBox.bottom)
            close()
        }
        drawPath(path = pilePath, color = sandColor)
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
