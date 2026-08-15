package cu.christianrvdv.sumador.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_API_URL = "https://api.github.com/repos/christianrvdv/Sumador/releases/latest"
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Comprueba si hay una nueva versión disponible en GitHub.
     * @return UpdateInfo si hay actualización, null si no o si hay error.
     */
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

    /**
     * Descarga el APK y lanza el instalador.
     * Emite progreso a través de downloadState.
     * @return true si la descarga e instalación fueron exitosas, false en caso de error.
     */
    suspend fun downloadAndInstall(downloadUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadState.update { DownloadState.Downloading(0f) }

            val fileName = "sumador_update.apk"
            val outputFile = File(context.cacheDir, fileName)
            if (outputFile.exists()) outputFile.delete()

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.connect()

            val contentLength = connection.contentLength.toLong()
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                    _downloadState.update { DownloadState.Downloading(progress.coerceIn(0f, 1f)) }
                }
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()

            if (outputFile.exists()) {
                _downloadState.update { DownloadState.Completed }
                installApk(outputFile)
                // Resetear estado después de un breve retraso para que el diálogo de progreso desaparezca
                // pero no es necesario, ya que Completed no muestra diálogo.
                resetState()
                return@withContext true
            } else {
                _downloadState.update { DownloadState.Error("El archivo descargado no existe") }
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al descargar o instalar", e)
            _downloadState.update { DownloadState.Error(e.message ?: "Error desconocido") }
            return@withContext false
        }
    }

    private fun installApk(apkFile: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
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

    // Data classes para la respuesta JSON de GitHub
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