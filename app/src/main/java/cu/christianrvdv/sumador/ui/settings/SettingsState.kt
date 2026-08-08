// SettingsState.kt
package cu.christianrvdv.sumador.ui.settings

enum class ThemeOption {
    LIGHT,
    DARK,
    SYSTEM
}

enum class CurrencySymbol(val symbol: String) {
    PESO("$"),
    CUP("CUP"),
    MN("MN")
}

data class SettingsState(
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val currencySymbol: CurrencySymbol = CurrencySymbol.PESO,
    val sortAscending: Boolean = true
)