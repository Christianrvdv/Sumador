package cu.christianrvdv.sumador.ui.sumador

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import cu.christianrvdv.sumador.data.database.Converters
import cu.christianrvdv.sumador.data.database.SavedSumDao
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import cu.christianrvdv.sumador.ui.settings.CurrencySymbol
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

private val Context.sumadorDataStore by preferencesDataStore(name = "sumador_state")

@HiltViewModel
class SumadorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedSumDao: SavedSumDao
) : ViewModel() {

    private val _cantidades = mutableStateMapOf<Int, String>()

    private val _state = MutableStateFlow(SumadorState(cantidades = emptyMap(), total = 0L))
    val state: StateFlow<SumadorState> = _state.asStateFlow()

    private var currentCurrency: CurrencySymbol = CurrencySymbol.PESO
    private var autoSaveEnabled = true
    private var useCoins: Boolean = false

    fun setCurrency(currency: CurrencySymbol) {
        if (currentCurrency != currency) {
            currentCurrency = currency
            _cantidades.clear()
            cargarCantidades(currency, useCoins)
        } else {
            cargarCantidades(currency, useCoins)
        }
    }

    fun setAutoSave(enabled: Boolean) {
        autoSaveEnabled = enabled
    }

    fun setUseCoins(enabled: Boolean) {
        if (useCoins != enabled) {
            useCoins = enabled
            _cantidades.clear()
            cargarCantidades(currentCurrency, useCoins)
        }
    }

    private fun cargarCantidades(currency: CurrencySymbol, useCoins: Boolean) {
        viewModelScope.launch {
            try {
                val prefs = context.sumadorDataStore.data.first()
                val denoms = getDenominations(currency, useCoins)
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
        val denoms = getDenominations(currentCurrency, useCoins)
        val total = denoms.sumOf { denom ->
            val cantidad = _cantidades[denom]?.toIntOrNull() ?: 0
            denom.toLong() * cantidad
        }
        _state.update {
            it.copy(total = total, cantidades = _cantidades.toMap())
        }
    }

    fun resetear() {
        val denoms = getDenominations(currentCurrency, useCoins)
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
                    val denoms = getDenominations(currentCurrency, useCoins)
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

    fun saveCurrentSum(name: String, total: Long, denominationsMap: Map<Int, Int>) {
        viewModelScope.launch {
            try {
                val entity = SavedSumEntity(
                    name = name,
                    timestamp = Date(),
                    total = total,
                    denominationsMap = Converters().fromMapToString(denominationsMap)
                )
                savedSumDao.insert(entity)
            } catch (e: Exception) {
                Log.e("SumadorViewModel", "Error saving sum", e)
            }
        }
    }
}