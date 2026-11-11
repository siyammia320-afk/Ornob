package com.example

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.system.exitProcess
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object a {
    fun b1(): String = String(Base64.decode("aHR0cHM6Ly9saW1pdGVkLmZhY2Vib29rLmNvbS8=", Base64.DEFAULT), Charsets.UTF_8).trim()
    
    fun p(): String = String(Base64.decode("ZmIudG9vLnByby54", Base64.DEFAULT), Charsets.UTF_8).trim() 
    
    fun n(): String = String(Base64.decode("RkIgVE9PTCBQUk8=", Base64.DEFAULT), Charsets.UTF_8).trim() 

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun c(context: Context) {
        // Bypassed
    }

    suspend fun checkStatusAndDevice(deviceId: String): String = withContext(Dispatchers.IO) {
        try {
            // 1. Check Global Status
            val request = okhttp3.Request.Builder().url(z.STATUS_LINK).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()?.trim() ?: ""
            response.close()
            
            if (body.isEmpty()) return@withContext "GLOBAL_OFF"
            val json = JSONObject(body)
            if (json.optString("status", "OFF") != "ON") {
                return@withContext "GLOBAL_OFF"
            }

            // 2. Check Device Approval
            val dbUrl = z.STATUS_LINK.replace("app_status.json", "")
            val reqDevice = okhttp3.Request.Builder().url("${dbUrl}approved_devices/$deviceId.json").build()
            val resDevice = client.newCall(reqDevice).execute()
            val bodyDevice = resDevice.body?.string()?.trim() ?: "null"
            resDevice.close()

            if (bodyDevice == "true" || bodyDevice == "\"true\"") {
                return@withContext "APPROVED_-1|Unknown"
            }
            
            if (bodyDevice == "null" || bodyDevice.isEmpty()) {
                return@withContext "NOT_FOUND|Unknown"
            }

            try {
                val deviceJson = JSONObject(bodyDevice)
                val status = deviceJson.optString("status", "")
                val expiry = deviceJson.optLong("expiry", -1L)
                val name = deviceJson.optString("name", "Unknown")

                if (status == "banned") {
                    return@withContext "BANNED|$name"
                } else if (status == "active") {
                    if (expiry == -1L || System.currentTimeMillis() < expiry) {
                        return@withContext "APPROVED_${expiry}|$name"
                    } else {
                        return@withContext "EXPIRED_${expiry}|$name"
                    }
                } else {
                    return@withContext "NOT_FOUND|Unknown"
                }
            } catch (e: Exception) {
                return@withContext "NOT_FOUND|Unknown"
            }
        } catch (e: Exception) {
            "NETWORK_ERROR"
        }
    }
}
