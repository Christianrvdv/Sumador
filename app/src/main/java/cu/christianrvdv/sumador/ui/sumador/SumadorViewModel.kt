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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.sumadorDataStore by preferencesDataStore(name = "sumador_state")

class SumadorViewModel(private val context: Context) : ViewModel() {

    private fun getKeyForDenom(denom: Int) = stringPreferencesKey("cantidad_$denom")

    private val _cantidades = mutableStateMapOf<Int, String>().apply {
        denominaciones.forEach { this[it] = "" }
    }

    private val _state = MutableStateFlow(
        SumadorState(cantidades = _cantidades.toMap(), total = 0L)
    )
    val state: StateFlow<SumadorState> = _state.asStateFlow()

    // Bandera para controlar la persistencia
    private var autoSaveEnabled = true

    init {
        // Cargar estado guardado (siempre al inicio, independientemente de autoSave)
        viewModelScope.launch {
            context.sumadorDataStore.data
                .catch { exception ->
                    Log.e("SumadorViewModel", "Error reading state", exception)
                    emit(emptyPreferences())
                }
                .collect { prefs ->
                    try {
                        denominaciones.forEach { denom ->
                            val saved = prefs[getKeyForDenom(denom)]
                            if (saved != null) {
                                _cantidades[denom] = saved
                            }
                        }
                        calcularTotal()
                    } catch (e: Exception) {
                        Log.e("SumadorViewModel", "Error parsing saved state", e)
                    }
                }
        }
    }

    /**
     * Actualiza la bandera de auto-save desde el exterior (UI)
     */
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
                    prefs[getKeyForDenom(denominacion)] = valor
                }
            } catch (e: Exception) {
                Log.e("SumadorViewModel", "Error saving amount for $denominacion", e)
            }
        }
    }

    private fun calcularTotal() {
        val total = denominaciones.sumOf { denom ->
            val cantidad = _cantidades[denom]?.toIntOrNull() ?: 0
            denom.toLong() * cantidad
        }
        _state.update { it.copy(total = total, cantidades = _cantidades.toMap()) }
    }

    fun resetear() {
        denominaciones.forEach { _cantidades[it] = "" }
        calcularTotal()
        if (autoSaveEnabled) {
            guardarReset()
        }
    }

    private fun guardarReset() {
        viewModelScope.launch {
            try {
                context.sumadorDataStore.edit { prefs ->
                    denominaciones.forEach { denom ->
                        prefs[getKeyForDenom(denom)] = ""
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