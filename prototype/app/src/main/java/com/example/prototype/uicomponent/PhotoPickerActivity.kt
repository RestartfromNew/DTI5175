package com.example.prototype.uicomponent

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.prototype.uicomponent.PhotoPickerScreen
import com.example.prototype.viewmodel.PhotoViewModel


class PhotoPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = ViewModelProvider(this)
                .get(PhotoViewModel::class.java)
            PhotoPickerRoute(viewModel)
        }
    }
}


