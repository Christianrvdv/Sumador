package cu.christianrvdv.sumador

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import cu.christianrvdv.sumador.utils.UpdateManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SumadorApplication : Application() {

    // Variable para almacenar la actualización pendiente de instalar
    var pendingUpdate: UpdateManager.UpdateInfo? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "update_channel",
                getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.update_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}