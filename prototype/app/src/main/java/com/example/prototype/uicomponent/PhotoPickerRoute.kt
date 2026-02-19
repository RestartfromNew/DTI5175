package com.example.prototype.uicomponent
import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import com.example.prototype.viewmodel.PhotoViewModel
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
/*
*
拿 ViewModel，订阅 StateFlow，处理系统回调（比如 ActivityResult），触发 ViewModel 事件
把数据和事件“注入”给 Screen
它是“有逻辑”的。*/
@Composable
fun PhotoPickerRoute(viewModel: PhotoViewModel) {

    val cachedPath by viewModel.cachedPath
        .collectAsStateWithLifecycle()

    val pickImageLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { viewModel.onPhotoSelected(it) }
        }

    PhotoPickerScreen(
        cachedPath = cachedPath,
        onPickClick = {
            pickImageLauncher.launch("image/*")
            Log.d("DEBUG", "Button clicked")

        }
    )
}
