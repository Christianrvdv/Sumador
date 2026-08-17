// di/BackupModule.kt
package cu.christianrvdv.sumador.di

import android.app.backup.BackupManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideBackupManager(@ApplicationContext context: Context): BackupManager {
        return BackupManager(context)
    }
}