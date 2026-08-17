package cu.christianrvdv.sumador.data

import cu.christianrvdv.sumador.data.database.CustomDenominationEntity
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import cu.christianrvdv.sumador.ui.settings.SettingsState

data class BackupData(
    val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val settings: SettingsState,
    val savedSums: List<SavedSumEntity>,
    val customDenominations: List<CustomDenominationEntity>
)