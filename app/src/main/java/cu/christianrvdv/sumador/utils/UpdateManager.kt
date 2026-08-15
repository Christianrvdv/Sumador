package cu.christianrvdv.sumador.utils

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_API_URL =
            "https://api.github.com/repos/christianrvdv/Sumador/releases/latest"

        // Cliente OkHttp con timeouts más largos
        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    sealed class UpdateResult {
        data class Success(val info: UpdateInfo) : UpdateResult()
        data class Error(val throwable: Throwable) : UpdateResult()
        object NoUpdate : UpdateResult()
        object NetworkError : UpdateResult()
    }

    suspend fun checkForUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(GITHUB_API_URL)
            .get()
            .build()

        val call = okHttpClient.newCall(request)

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Error en la respuesta de GitHub: ${response.code}")
                return@withContext UpdateResult.Error(Exception("HTTP ${response.code}"))
            }

            val json = response.body?.string() ?: run {
                Log.e(TAG, "Respuesta vacía de GitHub")
                return@withContext UpdateResult.Error(Exception("Empty response"))
            }

            Log.d(TAG, "Respuesta de GitHub: $json")

            val gson = GsonBuilder().setLenient().create()
            val release = gson.fromJson(json, Release::class.java)

            val tagName = release.tagName
            if (tagName.isNullOrEmpty()) {
                Log.e(TAG, "tag_name es null o vacío en la respuesta")
                return@withContext UpdateResult.Error(Exception("tag_name missing"))
            }

            val latestVersion = tagName.removePrefix("v")
            val currentVersion =
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

            Log.d(TAG, "Versión actual: $currentVersion, última: $latestVersion")

            if (compareVersions(latestVersion, currentVersion) > 0) {
                val assets = release.assets
                if (assets.isNullOrEmpty()) {
                    Log.e(TAG, "No hay assets en el release")
                    return@withContext UpdateResult.Error(Exception("No assets found"))
                }
                val apkAsset = assets.firstOrNull { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    Log.d(
                        TAG,
                        "Nueva versión $latestVersion, URL descarga: ${apkAsset.browserDownloadUrl}"
                    )
                    return@withContext UpdateResult.Success(
                        UpdateInfo(
                            version = latestVersion,
                            downloadUrl = apkAsset.browserDownloadUrl
                        )
                    )
                } else {
                    Log.e(TAG, "No se encontró un archivo APK en los assets")
                    return@withContext UpdateResult.Error(Exception("No APK found"))
                }
            } else {
                Log.d(TAG, "La versión actual ($currentVersion) ya es la más reciente.")
                return@withContext UpdateResult.NoUpdate
            }
        } catch (e: CancellationException) {
            // La corrutina fue cancelada (por ejemplo, al destruir la actividad)
            Log.d(TAG, "Verificación de actualización cancelada")
            call.cancel() // Cancela la petición en curso
            throw e // Propaga la cancelación
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Error de red al verificar actualización", e)
            return@withContext UpdateResult.NetworkError
        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S al verificar actualización", e)
            return@withContext UpdateResult.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar actualización", e)
            return@withContext UpdateResult.Error(e)
        }
    }

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
        @SerializedName("tag_name") val tagName: String?,
        val assets: List<Asset>?
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