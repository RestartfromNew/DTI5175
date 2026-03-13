package com.example.chatpart.network

import android.content.Context
import android.util.Log
import androidx.core.view.DragAndDropPermissionsCompat.request
import com.example.chatpart.auth.SessionManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object APIService {

    private val client = OkHttpClient()

    fun login(
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {

        val json = JSONObject()
        json.put("user_info", email)
        json.put("password", password)

        // ⭐ 打印发送的数据
        Log.d("API_LOGIN", "Email: $email")
        Log.d("API_LOGIN", "Password: $password")
        Log.d("API_LOGIN", "JSON Body: $json")

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://aittelegramapi.uk/login")
            .post(body)
            .build()

        Log.d("API_LOGIN", "Sending request to: https://aittelegramapi.uk/login")

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {

                Log.e("API_LOGIN", "Request failed: ${e.message}")

                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {

                val responseBody = response.body?.string()

                // ⭐ 打印服务器返回
                Log.d("API_LOGIN", "Response code: ${response.code}")
                Log.d("API_LOGIN", "Response body: $responseBody")

                if (response.isSuccessful) {
                    callback(true, responseBody)
                } else {
                    callback(false, responseBody)
                }
            }
        })
    }
    fun register(
        username: String,
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {

        val json = JSONObject()
        json.put("username", username)
        json.put("email", email)
        json.put("password", password)

        // ⭐ 打印发送数据
        Log.d("API_REGISTER", "Username: $username")
        Log.d("API_REGISTER", "Email: $email")
        Log.d("API_REGISTER", "Password: $password")
        Log.d("API_REGISTER", "JSON Body: $json")

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://aittelegramapi.uk/register")
            .post(body)
            .build()

        Log.d("API_REGISTER", "Sending request to: https://aittelegramapi.uk/register")

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {

                Log.e("API_REGISTER", "Request failed: ${e.message}")

                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {

                val responseBody = response.body?.string()

                // ⭐ 打印服务器返回
                Log.d("API_REGISTER", "Response code: ${response.code}")
                Log.d("API_REGISTER", "Response body: $responseBody")

                if (response.isSuccessful) {

                    try {

                        val json = JSONObject(responseBody ?: "")

                        if (json.has("message") && json.getString("message") == "success") {

                            callback(true, responseBody)

                        } else if (json.has("error")) {

                            callback(false, json.getString("error"))

                        } else {

                            callback(false, "Unknown response")

                        }

                    } catch (e: Exception) {

                        callback(false, "Parse error")

                    }

                } else {

                    callback(false, responseBody)

                }
            }
        })
    }
    fun googleLogin(
        context: Context,
        idToken: String
    ) {

        val json = JSONObject()
        json.put("id_token", idToken)

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.198.88.193:5001/google")
            .post(body)
            .build()

        Log.d("API_LOGIN", "Sending Google login request")

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("API_LOGIN", "Google login failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {

                val responseBody = response.body?.string()

                Log.d("API_LOGIN", "Server response: $responseBody")

                if (response.isSuccessful && responseBody != null) {

                    try {

//                        val root = JSONObject(responseBody)
//
//                        val data = root.getJSONObject("data")
//
//                        val accessToken = data.getString("access_token")
//                        val refreshToken = data.getString("refresh_token")
//
//                        val user = data.getJSONObject("user")
//
//                        val sessionManager = SessionManager(context)
//
//                        sessionManager.saveSession(
//                            accessToken,
//                            refreshToken,
//                            user.getString("id"),
//                            user.getString("username"),
//                            user.getString("email")
//                        )

                        Log.d("API_LOGIN", "Google login session saved")

                    } catch (e: Exception) {

                        Log.e("API_LOGIN", "JSON parse error: ${e.message}")

                    }
                }
            }
        })
    }
}