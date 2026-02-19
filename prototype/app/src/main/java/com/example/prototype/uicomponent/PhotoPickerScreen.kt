package com.example.prototype.uicomponent


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PhotoPickerScreen(
    cachedPath: String?,
    onPickClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Button(onClick = onPickClick) {
            Text("Select Photo")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Cached path:")
        Text(cachedPath ?: "No image selected")
        Spacer(modifier = Modifier.height(20.dp))

        if (cachedPath != null) {

            Text("Selected image:")

            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = cachedPath,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

        } else {

            Text("No image selected")

        }
    }
}
