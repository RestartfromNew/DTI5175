package com.example.chatpart.auth

import android.util.Log
import com.example.chatpart.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Token Sync Manager / Token 同步管理器
 *
 * After user logs in, sends Firebase ID Token to your server for sync.
 * 在用户登录成功后，将 Firebase ID Token 发送到用户的服务器进行同步。
 * Server can verify token and return custom session or complete user binding.
 * 服务器可以验证 token 并返回自定义 session 或完成用户绑定。
 */
class TokenSyncManager {

    companion object {
        private const val TAG = "TokenSyncManager"

        // API endpoints / API 端点
        private const val ENDPOINT_LOGIN_SYNC = "/api/auth/login-sync"
        private const val ENDPOINT_REFRESH_TOKEN = "/api/auth/refresh-token"
    }

    private val baseUrl: String = BuildConfig.SYNC_SERVER_URL

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Sync login info to server / 同步登录信息到服务器
     *
     * @param firebaseToken Firebase ID Token
     * @param userEmail User email / 用户邮箱
     * @param userName User display name / 用户显示名称
     * @param userId Firebase User ID
     * @return Sync result with custom token from server (if any) / 同步结果，包含服务器返回的自定义 token（如果有）
     */
    suspend fun syncLoginToServer(
        firebaseToken: String,
        userEmail: String,
        userName: String,
        userId: String
    ): Result<SyncResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Syncing login to server: $baseUrl$ENDPOINT_LOGIN_SYNC")

            val requestBody = buildLoginSyncRequest(
                firebaseToken = firebaseToken,
                userEmail = userEmail,
                userName = userName,
                userId = userId
            )

            val request = Request.Builder()
                .url("$baseUrl$ENDPOINT_LOGIN_SYNC")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "Server sync failed: ${response.code} - ${response.message}")
                    Log.e(TAG, "Response body: $responseBody")
                    return@withContext Result.failure(
                        ServerSyncException(
                            "Server sync failed: ${response.code}",
                            response.code,
                            responseBody
                        )
                    )
                }

                Log.d(TAG, "Server sync success: $responseBody")

                val syncResponse = gson.fromJson(responseBody, SyncResponse::class.java)
                Result.success(syncResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Refresh server token / 刷新服务器 token
     *
     * @param serverToken Current server token / 当前服务器返回的 token
     * @return New server token / 新的服务器 token
     */
    suspend fun refreshServerToken(serverToken: String): Result<RefreshTokenResponse> =
        withContext(Dispatchers.IO) {
            try {
                val requestBody = JsonObject().apply {
                    addProperty("token", serverToken)
                }.toString()

                val request = Request.Builder()
                    .url("$baseUrl$ENDPOINT_REFRESH_TOKEN")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            ServerSyncException(
                                "Token refresh failed: ${response.code}",
                                response.code,
                                responseBody
                            )
                        )
                    }

                    val refreshResponse = gson.fromJson(
                        responseBody,
                        RefreshTokenResponse::class.java
                    )
                    Result.success(refreshResponse)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh error", e)
                Result.failure(e)
            }
        }

    private fun buildLoginSyncRequest(
        firebaseToken: String,
        userEmail: String,
        userName: String,
        userId: String
    ): okhttp3.RequestBody {
        val json = JsonObject().apply {
            addProperty("firebase_token", firebaseToken)
            addProperty("user_email", userEmail)
            addProperty("user_name", userName)
            addProperty("user_id", userId)
            addProperty("timestamp", System.currentTimeMillis())
            addProperty("app_version", BuildConfig.VERSION_NAME)
        }
        return json.toString().toRequestBody(mediaType)
    }

    /**
     * Server sync response / 服务器同步响应
     */
    data class SyncResponse(
        val success: Boolean,
        val server_token: String? = null,      // Server custom token / 服务器自定义 token
        val session_id: String? = null,         // Session ID / 会话 ID
        val user_binding_id: String? = null,    // User binding ID (if server creates new user) / 用户绑定 ID（如果服务器创建了新用户）
        val message: String? = null,
        val expires_in: Long? = null           // Token expiration time in seconds / token 过期时间（秒）
    )

    /**
     * Token refresh response / Token 刷新响应
     */
    data class RefreshTokenResponse(
        val success: Boolean,
        val server_token: String? = null,
        val expires_in: Long? = null
    )

    /**
     * Server sync exception / 服务器同步异常
     */
    class ServerSyncException(
        message: String,
        val statusCode: Int,
        val responseBody: String
    ) : Exception(message)
}
