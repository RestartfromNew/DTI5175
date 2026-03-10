package com.example.chatpart

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SettingsVoice
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Peach = Color(0xFFFF9D85)
val Lavender = Color(0xB6A6FF)
val AccentPink = Color(0xFFFF85A2)
val SoftWhite = Color(0xFFFFF9F6)
val DarkText = Color(0xFF2D2D2D)

@Composable
fun OnboardingScreen(onSkip: () -> Unit, onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SoftWhite, Color(0xFFF3F0FF))))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.8f),
                    shadowElevation = 2.dp
                ) {
                    Icon(
                        Icons.Rounded.SettingsVoice,
                        contentDescription = null,
                        tint = Peach,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                TextButton(onClick = onSkip) {
                    Text("Skip", color = Lavender, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(300.dp)) {
                    drawCircle(Peach.copy(alpha = 0.1f), radius = 150.dp.toPx(), center = center.copy(x = center.x - 40f))
                    drawCircle(Lavender.copy(alpha = 0.1f), radius = 100.dp.toPx(), center = center.copy(x = center.x + 60f, y = center.y - 60f))
                }

                MarshmallowRobotCanvas()

                FloatingMusicNote(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 80.dp, y = (-100).dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Marshmallow Voice",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkText
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Customize your AI friend to sound as sweet or as cool as you like!",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    lineHeight = 26.sp
                )

                Spacer(Modifier.height(40.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp, 10.dp).clip(CircleShape).background(Peach))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Lavender.copy(alpha = 0.3f)))
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Peach),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Next", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun MarshmallowRobotCanvas() {
    Canvas(modifier = Modifier.size(200.dp, 260.dp)) {
        val w = size.width
        val h = size.height

        val bodyPath = Path().apply {
            moveTo(w * 0.1f, h * 0.3f)
            cubicTo(w * 0.1f, h * 0.1f, w * 0.9f, h * 0.1f, w * 0.9f, h * 0.3f) // 顶部圆弧
            lineTo(w * 0.9f, h * 0.8f)
            cubicTo(w * 0.9f, h * 1.0f, w * 0.1f, h * 1.0f, w * 0.1f, h * 0.8f) // 底部圆弧
            close()
        }

        drawPath(
            path = bodyPath,
            brush = Brush.linearGradient(listOf(Color.White, Color(0xFFFFE9E3)))
        )

        drawCircle(DarkText, radius = 6.dp.toPx(), center = Offset(w * 0.35f, h * 0.45f))
        drawCircle(DarkText, radius = 6.dp.toPx(), center = Offset(w * 0.65f, h * 0.45f))

        drawArc(
            color = DarkText,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(w * 0.42f, h * 0.52f),
            size = Size(w * 0.16f, h * 0.08f)
        )

        drawCircle(Peach.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = Offset(w * 0.25f, h * 0.52f))
        drawCircle(Peach.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = Offset(w * 0.75f, h * 0.52f))
    }
}

@Composable
fun FloatingMusicNote(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(
        modifier = modifier
            .offset(y = dy.dp)
            .rotate(12f)
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Lavender, AccentPink)))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
    }
}