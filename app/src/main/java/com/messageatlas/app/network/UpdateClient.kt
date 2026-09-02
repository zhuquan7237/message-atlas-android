package com.messageatlas.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class AppUpdate(
    val version: String,
    val notes: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

class UpdateClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val LATEST_URL =
            "https://api.github.com/repos/zhuquan7237/message-atlas-android/releases/latest"

        /** 比较语义化版本，>0 表示 remote 更新；忽略 v 前缀与 -后缀 */
        fun isRemoteNewer(remote: String, local: String): Boolean {
            fun parse(value: String) = value.trim().removePrefix("v").removePrefix("V")
                .substringBefore('-').split('.').map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            val r = parse(remote); val l = parse(local)
            for (i in 0 until maxOf(r.size, l.size)) {
                val rv = r.getOrElse(i) { 0 }; val lv = l.getOrElse(i) { 0 }
                if (rv != lv) return rv > lv
            }
            return false
        }
    }

    suspend fun latestRelease(): AppUpdate? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_URL)
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val root = json.parseToJsonElement(response.body?.string() ?: return@use null).jsonObject
            parseRelease(root)
        }
    }

    private fun parseRelease(root: JsonObject): AppUpdate? {
        val tagName = root["tag_name"]?.jsonPrimitive?.contentOrNull ?: return null
        val notes = root["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val assets = root["assets"]?.jsonArray ?: return null
        for (asset in assets) {
            val item = asset as? JsonObject ?: continue
            val name = item["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (name.endsWith(".apk", ignoreCase = true)) {
                val url = item["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: continue
                val size = item["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
                return AppUpdate(version = tagName, notes = notes, downloadUrl = url, sizeBytes = size)
            }
        }
        return null
    }

    suspend fun download(update: AppUpdate, destination: File, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val part = File(destination.parentFile, destination.name + ".part")
            val request = Request.Builder().url(update.downloadUrl).get().build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "下载失败：HTTP ${response.code}" }
                val total = response.body?.contentLength() ?: update.sizeBytes
                response.body!!.byteStream().use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var done = 0L
                        var lastPercent = -1
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            done += read
                            if (total > 0) {
                                val percent = (done * 100 / total).toInt()
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent.coerceIn(0, 100))
                                }
                            }
                        }
                    }
                }
            }
            check(part.renameTo(destination)) { "下载文件重命名失败" }
            destination
        }
}
