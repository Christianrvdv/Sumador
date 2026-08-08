package cu.christianrvdv.sumador.ui.sumador

// Denominaciones disponibles (puedes modificarlas)
val denominaciones = listOf(10, 20, 50, 100, 1000, 2000, 5000)

// Estado inmutable de la pantalla
data class SumadorState(
    val cantidades: Map<Int, String> = emptyMap(), // denominación -> texto ingresado
    val total: Long = 0L // Total calculado (Long para evitar desbordamiento)
)