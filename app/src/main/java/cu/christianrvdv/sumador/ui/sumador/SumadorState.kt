package cu.christianrvdv.sumador.ui.sumador

import cu.christianrvdv.sumador.ui.settings.CurrencySymbol

/**
 * Obtiene la lista de denominaciones (billetes y monedas) para una divisa.
 * @param currency Divisa seleccionada
 * @param useCoins Si es true, incluye monedas; si es false, solo billetes
 */
fun getDenominations(currency: CurrencySymbol, useCoins: Boolean): List<Int> = when (currency) {
    CurrencySymbol.PESO -> {
        val bills = listOf(100, 300, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000)
        val coins = listOf(1, 2, 5, 20, 50)
        if (useCoins) (bills + coins).sorted() else bills
    }
    CurrencySymbol.USD -> {
        val bills = listOf(100, 200, 500, 1000, 2000, 5000, 10000)
        val coins = listOf(1, 5, 10, 25, 50)
        if (useCoins) (bills + coins).sorted() else bills
    }
    CurrencySymbol.EURO -> {
        val bills = listOf(500, 1000, 2000, 5000, 10000, 20000, 50000)
        val coins = listOf(1, 2, 5, 10, 20, 50)
        if (useCoins) (bills + coins).sorted() else bills
    }
}

data class SumadorState(
    val cantidades: Map<Int, String> = emptyMap(),
    val total: Long = 0L
)