package cu.christianrvdv.sumador.ui.sumador

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SumadorViewModel : ViewModel() {

    // Mapa mutable para almacenar las cantidades ingresadas
    private val _cantidades = mutableStateMapOf<Int, String>().apply {
        denominaciones.forEach { this[it] = "" }
    }

    // Estado observable (inmutable)
    private val _state = MutableStateFlow(
        SumadorState(cantidades = _cantidades.toMap(), total = 0L)
    )
    val state: StateFlow<SumadorState> = _state.asStateFlow()

    /**
     * Actualiza la cantidad de una denominación y recalcula el total automáticamente.
     */
    fun updateCantidad(denominacion: Int, valor: String) {
        _cantidades[denominacion] = valor
        calcularTotal() // Recalcular automáticamente
    }

    /**
     * Calcula el total sumando denominación * cantidad (si la cantidad es válida).
     * Se puede llamar desde un botón si se prefiere el cálculo manual.
     */
    fun calcularTotal() {
        val total = denominaciones.sumOf { denom ->
            val cantidad = _cantidades[denom]?.toIntOrNull() ?: 0
            denom.toLong() * cantidad // Convertir a Long para evitar overflow
        }
        _state.update { it.copy(total = total, cantidades = _cantidades.toMap()) }
    }
}