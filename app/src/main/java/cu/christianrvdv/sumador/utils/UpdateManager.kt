package cu.christianrvdv.sumador.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_API_URL = "https://api.github.com/repos/christianrvdv/Sumador/releases/latest"
    }

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Error en la respuesta de GitHub: $responseCode")
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val release = Gson().fromJson(response, Release::class.java)
            val latestVersion = release.tagName.removePrefix("v")
            val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

            if (compareVersions(latestVersion, currentVersion) > 0) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    Log.d(TAG, "Nueva versión $latestVersion, URL descarga: ${apkAsset.browserDownloadUrl}")
                    return@withContext UpdateInfo(
                        version = latestVersion,
                        downloadUrl = apkAsset.browserDownloadUrl
                    )
                } else {
                    Log.e(TAG, "No se encontró un archivo APK en los assets")
                }
            } else {
                Log.d(TAG, "La versión actual ($currentVersion) ya es la más reciente.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar actualización", e)
        }
        return@withContext null
    }

    // NUEVO: se pasa también la versión
    fun startBackgroundDownload(downloadUrl: String, version: String) {
        DownloadWorker.start(context, downloadUrl, version)
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = if (i < parts1.size) parts1[i] else 0
            val p2 = if (i < parts2.size) parts2[i] else 0
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    data class Release(
        @SerializedName("tag_name") val tagName: String,
        val assets: List<Asset>
    )

    data class Asset(
        val name: String,
        @SerializedName("browser_download_url") val browserDownloadUrl: String
    )

    data class UpdateInfo(
        val version: String,
        val downloadUrl: String
    )
}