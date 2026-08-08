package cu.christianrvdv.sumador.ui.sumador

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SumadorViewModel : ViewModel() {

    // Mapa mutable para las cantidades (solo interno)
    private val _cantidades = mutableStateMapOf<Int, String>().apply {
        denominaciones.forEach { this[it] = "" }
    }

    // Estado observable
    private val _state = MutableStateFlow(
        SumadorState(cantidades = _cantidades.toMap(), total = 0L)
    )
    val state: StateFlow<SumadorState> = _state.asStateFlow()

    /**
     * Actualiza la cantidad de una denominación y recalcula el total automáticamente.
     */
    fun updateCantidad(denominacion: Int, valor: String) {
        _cantidades[denominacion] = valor
        calcularTotal()
    }

    /**
     * Calcula el total sumando denominación * cantidad (convierte a Long).
     */
    fun calcularTotal() {
        val total = denominaciones.sumOf { denom ->
            val cantidad = _cantidades[denom]?.toIntOrNull() ?: 0
            denom.toLong() * cantidad
        }
        _state.update { it.copy(total = total, cantidades = _cantidades.toMap()) }
    }

    /**
     * Resetea todos los campos a vacío y recalcula el total (0).
     */
    fun resetear() {
        denominaciones.forEach { _cantidades[it] = "" }
        calcularTotal() // total = 0
    }
}