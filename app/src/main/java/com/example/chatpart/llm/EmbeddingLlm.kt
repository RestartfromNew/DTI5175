package com.example.chatpart.llm

import android.util.Log
import com.example.chatpart.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

interface EmbeddingClient {
    suspend fun embed(text: String): FloatArray
}

class EmbeddingLlm : EmbeddingClient {
    override suspend fun embed(text: String): FloatArray {

        return withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.API_Key

                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true


                val textObj = JSONObject().put("text", text)
                val partsArray = JSONArray().put(textObj)
                val contentObj = JSONObject().put("parts", partsArray)
                val requestObj = JSONObject()
                    .put("model", "models/text-embedding-004")
                    .put("content", contentObj)


                OutputStreamWriter(connection.outputStream).use { it.write(requestObj.toString()) }


                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseStr)
                    val valuesArray = jsonResponse.getJSONObject("embedding").getJSONArray("values")

                    val floatArray = FloatArray(valuesArray.length())
                    for (i in 0 until valuesArray.length()) {
                        floatArray[i] = valuesArray.getDouble(i).toFloat()
                    }
                    floatArray
                } else {
                    Log.e("Embedding", "Action denied: ${connection.responseCode}")
                    FloatArray(768) { 0f }
                }
            } catch (e: Exception) {
                Log.e("Embedding", "Unexpected vector: ${e.message}", e)
                FloatArray(768) { 0f }
            }
        }
    }
}