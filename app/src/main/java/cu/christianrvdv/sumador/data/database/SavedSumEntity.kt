// data/database/SavedSumEntity.kt
package cu.christianrvdv.sumador.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_sums")
data class SavedSumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val timestamp: Long,
    val total: Long,
    val denominationsMap: String
)