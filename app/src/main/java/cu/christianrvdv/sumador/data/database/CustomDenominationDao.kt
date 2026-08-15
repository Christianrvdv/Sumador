package cu.christianrvdv.sumador.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomDenominationDao {

    @Query("SELECT * FROM custom_denominations WHERE currency = :currency ORDER BY denomination ASC")
    fun getByCurrency(currency: String): Flow<List<CustomDenominationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomDenominationEntity)

    @Delete
    suspend fun delete(entity: CustomDenominationEntity)

    @Query("DELETE FROM custom_denominations WHERE currency = :currency")
    suspend fun deleteAllForCurrency(currency: String)
}