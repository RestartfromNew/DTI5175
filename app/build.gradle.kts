import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val aiKey = localProps.getProperty("API_Key") ?: ""
val minimaxApiKey = localProps.getProperty("MINIMAX_API_KEY") ?: ""
val minimaxGroupId = localProps.getProperty("MINIMAX_GROUP_ID") ?: ""
val minimaxBaseUrl = localProps.getProperty("MINIMAX_BASE_URL") ?: ""
val assemblyAiApiKey = localProps.getProperty("ASSEMBLYAI_API_KEY") ?: ""
val deepgramApiKey = localProps.getProperty("DEEPGRAM_API_KEY") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    kotlin("plugin.serialization") version "2.0.21"
}


android {
    namespace = "com.example.chatpart"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.datastore"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_Key", "\"$aiKey\"")
        buildConfigField("String", "MINIMAX_API_KEY", "\"$minimaxApiKey\"")
        buildConfigField("String", "MINIMAX_GROUP_ID", "\"$minimaxGroupId\"")
        buildConfigField("String", "MINIMAX_BASE_URL", "\"$minimaxBaseUrl\"")
        buildConfigField("String", "ASSEMBLYAI_API_KEY", "\"$assemblyAiApiKey\"")
        buildConfigField("String", "DEEPGRAM_API_KEY", "\"$deepgramApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.extended)

    // DataStore
    implementation(libs.datastore.preferences)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    // Kotlin Coroutines for Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Google Sign-In
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // Image Loading
    implementation(libs.coil.compose)

    // Reorderable (drag and drop)
    implementation(libs.reorderable)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // AI - Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")

    // MiniMax API - OkHttp & Gson
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}