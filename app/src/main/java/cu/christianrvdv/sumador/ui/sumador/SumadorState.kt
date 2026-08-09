package cu.christianrvdv.sumador.ui.sumador

import cu.christianrvdv.sumador.ui.settings.CurrencySymbol

fun getDenominations(currency: CurrencySymbol): List<Int> = when (currency) {
    CurrencySymbol.PESO -> listOf(5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000)
    CurrencySymbol.USD   -> listOf(1, 2, 5, 10, 20, 50, 100)
    CurrencySymbol.EURO  -> listOf(5, 10, 20, 50, 100, 200, 500)
}

data class SumadorState(
    val cantidades: Map<Int, String> = emptyMap(),
    val total: Long = 0L
)