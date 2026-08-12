// data/database/SavedSumDao.kt
package cu.christianrvdv.sumador.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savedSum: SavedSumEntity)

    @Delete
    suspend fun delete(savedSum: SavedSumEntity)

    @Query("SELECT * FROM saved_sums ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SavedSumEntity>>

    @Query("SELECT * FROM saved_sums WHERE id = :id")
    suspend fun getById(id: Long): SavedSumEntity?
}