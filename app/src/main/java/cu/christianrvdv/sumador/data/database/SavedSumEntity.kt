package cu.christianrvdv.sumador.data.database

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "saved_sums")
@Keep
data class SavedSumEntity(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("total") val total: Long,
    @SerializedName("denominationsMap") val denominationsMap: String
)