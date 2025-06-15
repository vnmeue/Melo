package com.malopieds.innertune.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    data class ReleaseInfo(
        val versionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    suspend fun getLatestVersionInfo(): Result<ReleaseInfo> =
        runCatching {
            val response = client.get("https://api.github.com/repos/vnmeue/Melo/releases/latest").bodyAsText()
            val json = JSONObject(response)
            val versionName = json.getString("name")
            val downloadUrl = json.getJSONArray("assets")
                .getJSONObject(0)
                .getString("browser_download_url")
            val releaseNotes = json.getString("body")
            lastCheckTime = System.currentTimeMillis()
            ReleaseInfo(versionName, downloadUrl, releaseNotes)
        }

    suspend fun getLatestVersionName(): Result<String> =
        getLatestVersionInfo().map { it.versionName }

    fun shouldCheckForUpdates(lastCheckTime: Long): Boolean {
        // Check for updates once every 24 hours
        return System.currentTimeMillis() - lastCheckTime > 24 * 60 * 60 * 1000
    }
}
