// data/database/SavedSumDao.kt
package cu.christianrvdv.sumador.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savedSum: SavedSumEntity)

    @Update
    suspend fun update(savedSum: SavedSumEntity)

    @Delete
    suspend fun delete(savedSum: SavedSumEntity)

    // Original: sin filtros
    @Query("SELECT * FROM saved_sums ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SavedSumEntity>>

    // Nuevo: con filtros opcionales
    @Query("""
        SELECT * FROM saved_sums 
        WHERE (:name IS NULL OR name LIKE '%' || :name || '%')
          AND (:dateFrom IS NULL OR timestamp >= :dateFrom)
          AND (:dateTo IS NULL OR timestamp <= :dateTo)
          AND (:totalMin IS NULL OR total >= :totalMin)
          AND (:totalMax IS NULL OR total <= :totalMax)
        ORDER BY timestamp DESC
    """)
    fun getFiltered(
        name: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        totalMin: Long? = null,
        totalMax: Long? = null
    ): Flow<List<SavedSumEntity>>

    @Query("SELECT * FROM saved_sums WHERE id = :id")
    suspend fun getById(id: Long): SavedSumEntity?
}