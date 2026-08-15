package cu.christianrvdv.sumador.di

import android.content.Context
import cu.christianrvdv.sumador.data.database.AppDatabase
import cu.christianrvdv.sumador.data.database.CustomDenominationDao
import cu.christianrvdv.sumador.data.database.SavedSumDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSavedSumDao(db: AppDatabase): SavedSumDao {
        return db.savedSumDao()
    }

    @Provides
    @Singleton
    fun provideCustomDenominationDao(db: AppDatabase): CustomDenominationDao {
        return db.customDenominationDao()
    }
}