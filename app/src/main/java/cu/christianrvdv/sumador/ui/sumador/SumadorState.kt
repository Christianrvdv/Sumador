package cu.christianrvdv.sumador.ui.sumador

// Denominaciones de billetes (ordenadas de menor a mayor)
val denominaciones = listOf(5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000)

data class SumadorState(
    val cantidades: Map<Int, String> = emptyMap(), // denominación -> texto ingresado
    val total: Long = 0L
)