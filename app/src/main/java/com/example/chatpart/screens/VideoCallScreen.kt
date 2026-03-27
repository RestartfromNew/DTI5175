package com.example.chatpart.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter

enum class VideoCallState {
    IDLE,
    LISTENING,
    GENERATING,
    PLAYING
}

@Composable
fun VideoCallScreen(
    characterImageUrl: String,
    videoUrl: String?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    var callState by remember { mutableStateOf(VideoCallState.IDLE) }

    LaunchedEffect(videoUrl) {
        callState = if (videoUrl.isNullOrBlank()) {
            VideoCallState.IDLE
        } else {
            VideoCallState.PLAYING
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            contentAlignment = Alignment.Center
        ) {
            when (callState) {
                VideoCallState.IDLE,
                VideoCallState.LISTENING,
                VideoCallState.GENERATING -> {
                    IdleCharacterView(
                        imageUrl = characterImageUrl,
                        state = callState
                    )
                }

                VideoCallState.PLAYING -> {
                    if (!videoUrl.isNullOrBlank()) {
                        VideoPlayerView(
                            videoUrl = videoUrl,
                            onCompleted = {
                                callState = VideoCallState.IDLE
                            }
                        )
                    }
                }
            }
        }

        Text(
            text = when (callState) {
                VideoCallState.IDLE -> "Tap to talk"
                VideoCallState.LISTENING -> "Listening..."
                VideoCallState.GENERATING -> "Generating reply..."
                VideoCallState.PLAYING -> "Speaking..."
            },
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )

        Button(
            onClick = {
                when (callState) {
                    VideoCallState.IDLE -> {
                        callState = VideoCallState.LISTENING
                        onStartListening()
                    }

                    VideoCallState.LISTENING -> {
                        callState = VideoCallState.GENERATING
                        onStopListening()
                    }

                    VideoCallState.GENERATING,
                    VideoCallState.PLAYING -> {
                    }
                }
            }
        ) {
            Text(
                text = when (callState) {
                    VideoCallState.IDLE -> "Talk"
                    VideoCallState.LISTENING -> "Stop"
                    VideoCallState.GENERATING -> "Generating..."
                    VideoCallState.PLAYING -> "Playing..."
                }
            )
        }
    }
}

@Composable
private fun IdleCharacterView(
    imageUrl: String,
    state: VideoCallState
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = imageUrl),
            contentDescription = "Character",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (state == VideoCallState.GENERATING) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun VideoPlayerView(
    videoUrl: String,
    onCompleted: () -> Unit
) {
    AndroidView(
        factory = {
            VideoView(it).apply {
                setVideoURI(Uri.parse(videoUrl))
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = false
                    start()
                }
                setOnCompletionListener {
                    onCompleted()
                }
            }
        },
        update = { videoView ->
            videoView.setVideoURI(Uri.parse(videoUrl))
            videoView.start()
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
        }
    }
}