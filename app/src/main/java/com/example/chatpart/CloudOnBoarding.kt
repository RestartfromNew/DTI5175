package com.example.chatpart

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CloudOnboardingScreen(onSkip: () -> Unit, onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF3F0FF), SoftWhite)))
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
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Lavender,
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
                    drawCircle(Lavender.copy(alpha = 0.15f), radius = 140.dp.toPx(), center = center.copy(x = center.x + 30f))
                    drawCircle(Peach.copy(alpha = 0.1f), radius = 100.dp.toPx(), center = center.copy(x = center.x - 50f, y = center.y + 40f))
                }

                CloudRobotCanvas()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "AI with Memory",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkText
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Your AI companion learns from conversations to better understand and mimic your expression.",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    lineHeight = 26.sp
                )

                Spacer(Modifier.height(40.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Peach.copy(alpha = 0.3f)))
                    Box(Modifier.size(32.dp, 10.dp).clip(CircleShape).background(Peach))
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
                        Text("Get Started", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun CloudRobotCanvas() {
    val cloudGradient = Brush.radialGradient(
        0.0f to Color.White,
        1.0f to Color(0xFFF0F4FF),
        center = Offset(100f, 100f)
    )

    Box(modifier = Modifier.size(260.dp, 200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(120.dp, 20.dp).align(Alignment.BottomCenter).offset(y = (-20).dp)) {
            drawOval(Color.Black.copy(alpha = 0.05f), style = Fill)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            drawCircle(cloudGradient, radius = 45.dp.toPx(), center = Offset(centerX - 45.dp.toPx(), centerY - 45.dp.toPx()))
            drawCircle(cloudGradient, radius = 40.dp.toPx(), center = Offset(centerX + 40.dp.toPx(), centerY - 35.dp.toPx()))
            drawCircle(cloudGradient, radius = 35.dp.toPx(), center = Offset(centerX + 70.dp.toPx(), centerY + 10.dp.toPx()))
            drawCircle(cloudGradient, radius = 38.dp.toPx(), center = Offset(centerX - 70.dp.toPx(), centerY + 5.dp.toPx()))
            drawRoundRect(
                brush = cloudGradient,
                topLeft = Offset(centerX - 80.dp.toPx(), centerY - 40.dp.toPx()),
                size = Size(160.dp.toPx(), 80.dp.toPx()),
                cornerRadius = CornerRadius(50.dp.toPx())
            )

            drawCircle(Color(0xFF4A4A4A), radius = 5.dp.toPx(), center = Offset(centerX - 25.dp.toPx(), centerY - 5.dp.toPx()))
            drawCircle(Color(0xFF4A4A4A), radius = 5.dp.toPx(), center = Offset(centerX + 25.dp.toPx(), centerY - 5.dp.toPx()))
            drawArc(
                color = Color(0xFF4A4A4A),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - 12.dp.toPx(), centerY + 5.dp.toPx()),
                size = Size(24.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(56.dp)
            )
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}