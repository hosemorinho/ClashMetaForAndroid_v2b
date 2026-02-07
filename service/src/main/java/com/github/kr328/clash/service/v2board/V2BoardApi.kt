package com.github.kr328.clash.service.v2board

import android.content.Context
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.service.store.AuthStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class V2BoardApi(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val authStore by lazy { AuthStore(context) }

    private fun baseUrl(): String {
        return try {
            val clazz = Class.forName("com.github.kr328.clash.BuildConfig")
            val field = clazz.getField("API_BASE_URL")
            field.get(null) as? String ?: "https://example.com/api/v1"
        } catch (e: Exception) {
            "https://example.com/api/v1"
        }
    }

    private fun buildRequest(endpoint: String, method: String, body: RequestBody? = null): Request {
        val builder = Request.Builder()
            .url(baseUrl() + endpoint)

        val authData = authStore.authData
        if (authData.isNotBlank()) {
            builder.header("Authorization", authData)
        }

        builder.header("Content-Type", "application/json")
        builder.header("Accept", "application/json")

        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(body ?: "{}".toRequestBody(JSON_MEDIA_TYPE))
        }

        return builder.build()
    }

    suspend fun post(endpoint: String, jsonBody: JsonObject = buildJsonObject {}): ApiResult<JsonElement> {
        return withContext(Dispatchers.IO) {
            try {
                val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = buildRequest(endpoint, "POST", body)
                val response = client.newCall(request).execute()
                parseResponse(response)
            } catch (e: IOException) {
                ApiResult.NetworkError(e.message ?: "Network error")
            } catch (e: Exception) {
                ApiResult.ServerError(e.message ?: "Unexpected error")
            }
        }
    }

    suspend fun get(endpoint: String): ApiResult<JsonElement> {
        return withContext(Dispatchers.IO) {
            try {
                val request = buildRequest(endpoint, "GET")
                val response = client.newCall(request).execute()
                parseResponse(response)
            } catch (e: IOException) {
                ApiResult.NetworkError(e.message ?: "Network error")
            } catch (e: Exception) {
                ApiResult.ServerError(e.message ?: "Unexpected error")
            }
        }
    }

    private fun parseResponse(response: Response): ApiResult<JsonElement> {
        val responseBody = response.body?.string() ?: ""

        return when (response.code) {
            200 -> {
                try {
                    val jsonElement = json.parseToJsonElement(responseBody)
                    val data = jsonElement.jsonObject["data"]
                    ApiResult.Success(data ?: jsonElement)
                } catch (e: Exception) {
                    ApiResult.Success(JsonPrimitive(responseBody))
                }
            }
            401, 403 -> {
                val message = try {
                    json.parseToJsonElement(responseBody).jsonObject["message"]?.jsonPrimitive?.content
                } catch (e: Exception) { null }
                ApiResult.AuthError(message ?: "Authentication failed")
            }
            422 -> {
                try {
                    val jsonElement = json.parseToJsonElement(responseBody).jsonObject
                    val message = jsonElement["message"]?.jsonPrimitive?.content ?: "Validation error"
                    val errorsJson = jsonElement["errors"]?.jsonObject
                    val errors = errorsJson?.mapValues { (_, v) ->
                        v.jsonArray.map { it.jsonPrimitive.content }
                    } ?: emptyMap()
                    ApiResult.ValidationError(message, errors)
                } catch (e: Exception) {
                    ApiResult.ValidationError(responseBody)
                }
            }
            in 500..599 -> {
                val message = try {
                    json.parseToJsonElement(responseBody).jsonObject["message"]?.jsonPrimitive?.content
                } catch (e: Exception) { null }
                ApiResult.ServerError(message ?: "Server error (${response.code})")
            }
            else -> {
                val message = try {
                    json.parseToJsonElement(responseBody).jsonObject["message"]?.jsonPrimitive?.content
                } catch (e: Exception) { null }
                ApiResult.ServerError(message ?: "Request failed (${response.code})")
            }
        }
    }

    inline fun <reified T> decode(element: JsonElement): T {
        return json.decodeFromJsonElement(element)
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
