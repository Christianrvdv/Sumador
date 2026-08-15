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

    private val currency: StateFlow<String> = savedStateHandle.getStateFlow("currency", "PESO")

    val denominations: StateFlow<List<CustomDenominationEntity>> =
        dao.getByCurrency(currency.value)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Añade una nueva denominación.
     * @param value valor nominal en la moneda (ej. 100 para un billete de 100)
     * @param isCoin true si es moneda, false si es billete
     */
    fun addDenomination(value: Int, isCoin: Boolean) {
        viewModelScope.launch {
            val valueInCents = if (isCoin) value else value * 100
            val current = dao.getByCurrency(currency.value).stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            ).value
            if (current.none { it.denomination == valueInCents && it.isCoin == isCoin }) {
                dao.insert(
                    CustomDenominationEntity(
                        currency = currency.value,
                        denomination = valueInCents,
                        isCoin = isCoin
                    )
                )
            }
        }
    }

    /**
     * Actualiza una denominación existente.
     * @param entity entidad a actualizar
     * @param newValue nuevo valor nominal en la moneda
     * @param newIsCoin nuevo tipo
     */
    fun updateDenomination(entity: CustomDenominationEntity, newValue: Int, newIsCoin: Boolean) {
        viewModelScope.launch {
            val newValueInCents = if (newIsCoin) newValue else newValue * 100
            val current = dao.getByCurrency(currency.value).stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            ).value
            if (current.none {
                    it.denomination == newValueInCents && it.isCoin == newIsCoin && it.id != entity.id
                }) {
                dao.insert(
                    entity.copy(
                        denomination = newValueInCents,
                        isCoin = newIsCoin
                    )
                )
            }
        }
    }

    fun deleteDenomination(entity: CustomDenominationEntity) {
        viewModelScope.launch {
            dao.delete(entity)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            dao.deleteAllForCurrency(currency.value)
        }
    }

    /**
     * Convierte un valor en centavos a su representación nominal para mostrar en la UI.
     * Para billetes: se divide entre 100. Para monedas: se muestra tal cual.
     */
    fun getDisplayValue(entity: CustomDenominationEntity): Int {
        return if (entity.isCoin) entity.denomination else entity.denomination / 100
    }
}