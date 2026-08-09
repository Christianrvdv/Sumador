package cu.christianrvdv.sumador.ui.sumador

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import cu.christianrvdv.sumador.ui.settings.CurrencySymbol
import kotlinx.coroutines.flow.first

private val Context.sumadorDataStore by preferencesDataStore(name = "sumador_state")

class SumadorViewModel(private val context: Context) : ViewModel() {

    // Mapa mutable de cantidades para la moneda actual
    private val _cantidades = mutableStateMapOf<Int, String>()

    private val _state = MutableStateFlow(SumadorState(cantidades = emptyMap(), total = 0L))
    val state: StateFlow<SumadorState> = _state.asStateFlow()

    private var currentCurrency: CurrencySymbol = CurrencySymbol.PESO
    private var autoSaveEnabled = true

    // Inicializar con la moneda por defecto (se cargarán los datos al setear)
    init {
        // No cargamos nada aquí; la UI llamará a setCurrency con la moneda guardada
    }

    fun setCurrency(currency: CurrencySymbol) {
        if (currentCurrency != currency) {
            currentCurrency = currency
            // Limpiar el mapa actual
            _cantidades.clear()
            // Cargar cantidades para la nueva moneda
            cargarCantidades(currency)
        }
    }

    private fun cargarCantidades(currency: CurrencySymbol) {
        viewModelScope.launch {
            try {
                val prefs = context.sumadorDataStore.data.first()
                val denoms = getDenominations(currency)
                denoms.forEach { denom ->
                    val key = stringPreferencesKey("${currency.name}_$denom")
                    val saved = prefs[key] ?: ""
                    _cantidades[denom] = saved
                }
                calcularTotal()
            } catch (e: Exception) {
                Log.e("SumadorViewModel", "Error loading amounts for $currency", e)
            }
        }
    }

    fun setAutoSave(enabled: Boolean) {
        autoSaveEnabled = enabled
    }

    fun updateCantidad(denominacion: Int, valor: String) {
        _cantidades[denominacion] = valor
        calcularTotal()
        if (autoSaveEnabled) {
            guardarCantidad(denominacion, valor)
        }
    }

    private fun guardarCantidad(denominacion: Int, valor: String) {
        viewModelScope.launch {
            try {
                context.sumadorDataStore.edit { prefs ->
                    val key = stringPreferencesKey("${currentCurrency.name}_$denominacion")
                    prefs[key] = valor
                }
            } catch (e: Exception) {
                Log.e("SumadorViewModel", "Error saving amount for $denominacion", e)
            }
        }
    }

    private fun calcularTotal() {
        val denoms = getDenominations(currentCurrency)
        val total = denoms.sumOf { denom ->
            val cantidad = _cantidades[denom]?.toIntOrNull() ?: 0
            denom.toLong() * cantidad
        }
        _state.update {
            it.copy(total = total, cantidades = _cantidades.toMap())
        }
    }

    fun resetear() {
        val denoms = getDenominations(currentCurrency)
        denoms.forEach { _cantidades[it] = "" }
        calcularTotal()
        if (autoSaveEnabled) {
            guardarReset()
        }
    }

    private fun guardarReset() {
        viewModelScope.launch {
            try {
                context.sumadorDataStore.edit { prefs ->
                    val denoms = getDenominations(currentCurrency)
                    denoms.forEach { denom ->
                        val key = stringPreferencesKey("${currentCurrency.name}_$denom")
                        prefs[key] = ""
                    }
                }
            } catch (e: Exception) {
                Log.e("SumadorViewModel", "Error resetting state", e)
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SumadorViewModel(context) as T
            }
        }
    }
}