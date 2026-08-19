package cu.christianrvdv.sumador.ui.settings

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
enum class ThemeOption {
    @SerializedName("LIGHT") LIGHT,
    @SerializedName("DARK") DARK,
    @SerializedName("SYSTEM") SYSTEM
}

@Keep
enum class CurrencySymbol(val symbol: String) {
    @SerializedName("PESO") PESO("$"),
    @SerializedName("USD") USD("USD"),
    @SerializedName("EURO") EURO("€")
}

@Keep
enum class LanguageOption {
    @SerializedName("ENGLISH") ENGLISH,
    @SerializedName("SPANISH") SPANISH,
    @SerializedName("SYSTEM") SYSTEM
}

@Keep
data class SettingsState(
    @SerializedName("theme") val theme: ThemeOption = ThemeOption.SYSTEM,
    @SerializedName("currencySymbol") val currencySymbol: CurrencySymbol = CurrencySymbol.PESO,
    @SerializedName("sortAscending") val sortAscending: Boolean = true,
    @SerializedName("autoSave") val autoSave: Boolean = true,
    @SerializedName("confirmClear") val confirmClear: Boolean = true,
    @SerializedName("language") val language: LanguageOption = LanguageOption.SYSTEM,
    @SerializedName("keepScreenOn") val keepScreenOn: Boolean = false,
    @SerializedName("useCoins") val useCoins: Boolean = false
)