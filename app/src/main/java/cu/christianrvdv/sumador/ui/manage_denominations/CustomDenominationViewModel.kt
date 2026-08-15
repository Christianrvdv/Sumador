package cu.christianrvdv.sumador.ui.manage_denominations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import cu.christianrvdv.sumador.data.database.CustomDenominationDao
import cu.christianrvdv.sumador.data.database.CustomDenominationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomDenominationViewModel @Inject constructor(
    private val dao: CustomDenominationDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // La moneda actual se recibe como argumento de navegación (clave "currency")
    private val currency: StateFlow<String> = savedStateHandle.getStateFlow("currency", "PESO")

    // Flujo de denominaciones personalizadas para esta moneda
    val denominations: StateFlow<List<CustomDenominationEntity>> =
        dao.getByCurrency(currency.value)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Añade una nueva denominación (si no existe ya).
     * Si ya existe, no hace nada (se puede manejar con un toast o mensaje).
     */
    fun addDenomination(value: Int) {
        viewModelScope.launch {
            // Evitar duplicados (opcional)
            val current = dao.getByCurrency(currency.value).stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            ).value
            if (current.none { it.denomination == value }) {
                dao.insert(CustomDenominationEntity(currency = currency.value, denomination = value))
            }
        }
    }

    /**
     * Actualiza una denominación existente.
     */
    fun updateDenomination(entity: CustomDenominationEntity, newValue: Int) {
        viewModelScope.launch {
            // Validar que no haya duplicado con otro (excepto el mismo)
            val current = dao.getByCurrency(currency.value).stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            ).value
            if (current.none { it.denomination == newValue && it.id != entity.id }) {
                dao.insert(entity.copy(denomination = newValue))
            }
        }
    }

    /**
     * Elimina una denominación.
     */
    fun deleteDenomination(entity: CustomDenominationEntity) {
        viewModelScope.launch {
            dao.delete(entity)
        }
    }

    /**
     * Restablece a las denominaciones predeterminadas eliminando todas las personalizadas
     * para la moneda actual.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            dao.deleteAllForCurrency(currency.value)
        }
    }
}