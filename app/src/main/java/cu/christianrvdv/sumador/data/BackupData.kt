package cu.christianrvdv.sumador.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import cu.christianrvdv.sumador.data.database.CustomDenominationEntity
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import cu.christianrvdv.sumador.ui.settings.SettingsState

@Keep
data class BackupData(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("exportDate") val exportDate: Long = System.currentTimeMillis(),
    @SerializedName("settings") val settings: SettingsState,
    @SerializedName("savedSums") val savedSums: List<SavedSumEntity>,
    @SerializedName("customDenominations") val customDenominations: List<CustomDenominationEntity>
)