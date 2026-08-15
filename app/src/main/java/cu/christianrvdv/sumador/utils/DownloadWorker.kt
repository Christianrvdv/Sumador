package cu.christianrvdv.sumador.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
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
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, downloadUrl: String) {
            val data = Data.Builder().putString(KEY_DOWNLOAD_URL, downloadUrl).build()
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
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = createNotificationBuilder()

        // Mostrar notificación inicial
        builder.setProgress(100, 0, true)
        notificationManager.notify(NOTIFICATION_ID, builder.build())

        return try {
            val fileName = "sumador_update.apk"
            val outputFile = File(applicationContext.cacheDir, fileName)
            outputFile.delete()

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
                    val progress = (totalBytesRead.toFloat() / contentLength.toFloat() * 100).toInt()
                    builder.setProgress(100, progress, false)
                    builder.setContentText("Descargando $progress%")
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()

            if (outputFile.exists()) {
                // Instalación
                builder.setContentText("Descarga completada. Instalando...")
                    .setProgress(0, 0, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())

                installApk(outputFile)
                Result.success()
            } else {
                builder.setContentText("Error: archivo no encontrado")
                    .setProgress(0, 0, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                Result.failure()
            }
        } catch (e: Exception) {
            builder.setContentText("Error: ${e.message}")
                .setProgress(0, 0, false)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
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
            .setContentTitle("Descargando actualización")
            .setContentText("Preparando...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true) // Mantiene la notificación persistente
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
    }

    private fun installApk(apkFile: File) {
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