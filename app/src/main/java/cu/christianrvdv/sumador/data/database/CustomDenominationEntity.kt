package cu.christianrvdv.sumador.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_denominations")
data class CustomDenominationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val currency: String, // "PESO", "USD", "EURO"
    val denomination: Int, // valor en centavos
    val isCoin: Boolean = false // true = moneda, false = billete
)