package cu.christianrvdv.sumador.data.database

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "custom_denominations")
@Keep
data class CustomDenominationEntity(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id") val id: Long = 0,
    @SerializedName("currency") val currency: String,
    @SerializedName("denomination") val denomination: Int,
    @SerializedName("isCoin") val isCoin: Boolean = false
)