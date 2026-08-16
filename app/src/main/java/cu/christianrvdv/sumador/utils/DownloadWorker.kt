package cu.christianrvdv.sumador.utils

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.*
import cu.christianrvdv.sumador.MainActivity
import cu.christianrvdv.sumador.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_TAG = "download_update"
        private const val KEY_DOWNLOAD_URL = "download_url"
        private const val KEY_VERSION = "version"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, downloadUrl: String, version: String) {
            val data = Data.Builder()
                .putString(KEY_DOWNLOAD_URL, downloadUrl)
                .putString(KEY_VERSION, version)
                .build()

            // Configurar reintentos con backoff exponencial
            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, workRequest)
        }

        // Obtener la ruta del APK para una versión
        fun getApkFile(context: Context, version: String): File {
            val updatesDir = File(context.filesDir, "updates")
            if (!updatesDir.exists()) updatesDir.mkdirs()
            return File(updatesDir, "sumador_v${version}.apk")
        }

        private fun getTempFile(context: Context, version: String): File {
            val updatesDir = File(context.filesDir, "updates")
            if (!updatesDir.exists()) updatesDir.mkdirs()
            return File(updatesDir, "sumador_v${version}.apk.tmp")
        }

        // Verificar si un APK es válido e instalable
        fun isApkValid(context: Context, apkFile: File): Boolean {
            return try {
                val pm = context.packageManager
                val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                pkgInfo != null
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val version = inputData.getString(KEY_VERSION) ?: return Result.failure()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val apkFile = getApkFile(applicationContext, version)
        val tempFile = getTempFile(applicationContext, version)

        // --- 1. Verificar si ya existe un APK válido ---
        if (apkFile.exists() && isApkValid(applicationContext, apkFile)) {
            Log.d("DownloadWorker", "APK válido ya existe, instalando directamente")
            installApk(apkFile)
            return Result.success()
        }

        // --- 2. Si existe pero es inválido, eliminarlo ---
        if (apkFile.exists()) {
            apkFile.delete()
            Log.d("DownloadWorker", "APK corrupto eliminado")
        }

        // --- 3. Si existe un .tmp de descarga anterior, eliminarlo ---
        if (tempFile.exists()) {
            tempFile.delete()
            Log.d("DownloadWorker", "Archivo temporal antiguo eliminado")
        }

        // --- 4. Preparar notificación ---
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val builder = if (hasNotificationPermission) {
            createNotificationBuilder()
        } else {
            null
        }

        if (builder != null) {
            builder.setProgress(100, 0, true)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }

        // --- 5. Descarga con manejo de errores ---
        return try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            val contentLength = connection.contentLength.toLong()
            if (contentLength <= 0) {
                Log.e("DownloadWorker", "Content-Length no disponible o cero")
                throw IOException("Invalid content length")
            }

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                // Verificar si el worker fue detenido
                if (isStopped) {
                    Log.d("DownloadWorker", "Worker detenido, cancelando descarga")
                    inputStream.close()
                    outputStream.close()
                    connection.disconnect()
                    tempFile.delete()
                    return Result.failure()
                }

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (builder != null && contentLength > 0) {
                    val progress = (totalBytesRead.toFloat() / contentLength.toFloat() * 100).toInt()
                    builder.setProgress(100, progress, false)
                    builder.setContentText(
                        applicationContext.getString(R.string.update_notification_downloading_progress, progress)
                    )
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Verificar que el tamaño descargado coincide con Content-Length
            if (totalBytesRead != contentLength) {
                Log.e("DownloadWorker", "Tamaño descargado ($totalBytesRead) no coincide con Content-Length ($contentLength)")
                tempFile.delete()
                throw IOException("Incomplete download")
            }

            // --- 6. Verificar integridad del APK descargado ---
            if (!isApkValid(applicationContext, tempFile)) {
                Log.e("DownloadWorker", "APK descargado no es válido")
                tempFile.delete()
                throw IOException("Invalid APK")
            }

            // --- 7. Renombrar de .tmp a .apk ---
            if (!tempFile.renameTo(apkFile)) {
                Log.e("DownloadWorker", "Error al renombrar archivo")
                tempFile.delete()
                throw IOException("Rename failed")
            }

            // --- 8. Notificar completado ---
            if (builder != null) {
                builder.setContentText(applicationContext.getString(R.string.update_notification_completed))
                    .setProgress(0, 0, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }

            // --- 9. Instalar APK ---
            installApk(apkFile)
            Result.success()

        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error durante la descarga", e)
            // Eliminar archivos temporales si existen
            if (tempFile.exists()) tempFile.delete()
            if (apkFile.exists() && !isApkValid(applicationContext, apkFile)) {
                apkFile.delete()
            }

            // Notificar error
            if (builder != null) {
                builder.setContentText(
                    applicationContext.getString(R.string.update_notification_error_generic, e.message ?: "")
                )
                    .setProgress(0, 0, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }

            // Reintentar si es un error recuperable (red, timeout, etc.)
            // WorkManager reintentará automáticamente según la política de backoff
            Result.retry()
        }
    }

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(applicationContext, "update_channel")
            .setContentTitle(applicationContext.getString(R.string.update_notification_title))
            .setContentText(applicationContext.getString(R.string.update_notification_preparing))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
    }

    private fun installApk(apkFile: File) {
        // Verificar si podemos solicitar instalación (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!applicationContext.packageManager.canRequestPackageInstalls()) {
                // Abrir configuración para habilitar instalación de fuentes desconocidas
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${applicationContext.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                applicationContext.startActivity(intent)

                // Notificar al usuario
                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val builder = NotificationCompat.Builder(applicationContext, "update_channel")
                    .setContentTitle(applicationContext.getString(R.string.update_notification_install_blocked_title))
                    .setContentText(applicationContext.getString(R.string.update_notification_install_blocked_text))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true)
                notificationManager.notify(NOTIFICATION_ID + 1, builder.build())
                return
            }
        }

        // Instalar APK
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                applicationContext,
                "${applicationContext.packageName}.fileprovider",
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
        applicationContext.startActivity(intent)
    }
}