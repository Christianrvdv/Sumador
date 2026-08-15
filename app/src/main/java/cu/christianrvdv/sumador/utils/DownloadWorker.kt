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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.*
import cu.christianrvdv.sumador.MainActivity
import cu.christianrvdv.sumador.R
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

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
            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, workRequest)
        }
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val version = inputData.getString(KEY_VERSION) ?: return Result.failure()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Verificar permiso de notificaciones (Android 13+)
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

        // Directorio persistente para APKs
        val updatesDir = File(applicationContext.filesDir, "updates")
        if (!updatesDir.exists()) updatesDir.mkdirs()

        val apkFile = File(updatesDir, "sumador_v${version}.apk")

        // Si el APK ya existe, instalar directamente
        if (apkFile.exists()) {
            if (builder != null) {
                builder.setContentText(applicationContext.getString(R.string.update_notification_already_downloaded))
                    .setProgress(0, 0, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
            installApk(apkFile)
            return Result.success()
        }

        // Mostrar notificación de progreso
        if (builder != null) {
            builder.setProgress(100, 0, true)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }

        return try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.connect()

            val contentLength = connection.contentLength.toLong()
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
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

            if (apkFile.exists()) {
                if (builder != null) {
                    builder.setContentText(applicationContext.getString(R.string.update_notification_completed))
                        .setProgress(0, 0, false)
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
                installApk(apkFile)
                Result.success()
            } else {
                if (builder != null) {
                    builder.setContentText(applicationContext.getString(R.string.update_notification_error_file_not_found))
                        .setProgress(0, 0, false)
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
                Result.failure()
            }
        } catch (e: Exception) {
            if (builder != null) {
                builder.setContentText(
                    applicationContext.getString(R.string.update_notification_error_generic, e.message ?: "")
                )
                    .setProgress(0, 0, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
            Result.failure()
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
        // Verificar si podemos solicitar instalación de paquetes (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!applicationContext.packageManager.canRequestPackageInstalls()) {
                // No tenemos permiso, abrir configuración para que el usuario lo habilite
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${applicationContext.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                applicationContext.startActivity(intent)

                // Mostrar notificación informativa (si hay permiso de notificaciones)
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

        // Si tenemos permiso, proceder con la instalación
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