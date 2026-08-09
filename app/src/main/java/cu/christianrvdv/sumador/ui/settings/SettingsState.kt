package cu.christianrvdv.sumador.ui.settings

enum class ThemeOption {
    LIGHT,
    DARK,
    SYSTEM
}

enum class CurrencySymbol(val symbol: String) {
    PESO("$"),
    USD("USD"),
    EURO("€")
}

enum class LanguageOption { ENGLISH, SPANISH, SYSTEM }

data class SettingsState(
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val currencySymbol: CurrencySymbol = CurrencySymbol.PESO,
    val sortAscending: Boolean = true,
    val autoSave: Boolean = true,
    val confirmClear: Boolean = true,
    val language: LanguageOption = LanguageOption.SYSTEM
)