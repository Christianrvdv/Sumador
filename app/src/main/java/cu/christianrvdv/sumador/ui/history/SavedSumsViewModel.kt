// ui/history/SavedSumsViewModel.kt
package cu.christianrvdv.sumador.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.christianrvdv.sumador.data.database.SavedSumDao
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilterState(
    val name: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val totalMin: Long? = null,
    val totalMax: Long? = null
)

@HiltViewModel
class SavedSumsViewModel @Inject constructor(
    private val savedSumDao: SavedSumDao
) : ViewModel() {

    // Estado de los filtros (internos y expuestos)
    private val _filterName = MutableStateFlow<String?>(null)
    private val _filterDateFrom = MutableStateFlow<Long?>(null)
    private val _filterDateTo = MutableStateFlow<Long?>(null)
    private val _filterTotalMin = MutableStateFlow<Long?>(null)
    private val _filterTotalMax = MutableStateFlow<Long?>(null)

    // Estado combinado para la UI
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // Lista filtrada en tiempo real
    val allSavedSums: Flow<List<SavedSumEntity>> = combine(
        _filterName,
        _filterDateFrom,
        _filterDateTo,
        _filterTotalMin,
        _filterTotalMax
    ) { name, dateFrom, dateTo, totalMin, totalMax ->
        savedSumDao.getFiltered(name, dateFrom, dateTo, totalMin, totalMax)
    }.flatMapLatest { it }

    fun setFilter(
        name: String?,
        dateFrom: Long?,
        dateTo: Long?,
        totalMin: Long?,
        totalMax: Long?
    ) {
        _filterName.value = name
        _filterDateFrom.value = dateFrom
        _filterDateTo.value = dateTo
        _filterTotalMin.value = totalMin
        _filterTotalMax.value = totalMax
        _filterState.value = FilterState(name, dateFrom, dateTo, totalMin, totalMax)
    }

    fun clearFilters() {
        setFilter(null, null, null, null, null)
    }

    // CRUD
    fun insert(savedSum: SavedSumEntity) {
        viewModelScope.launch { savedSumDao.insert(savedSum) }
    }

    fun update(savedSum: SavedSumEntity) {
        viewModelScope.launch { savedSumDao.update(savedSum) }
    }

    fun delete(savedSum: SavedSumEntity) {
        viewModelScope.launch { savedSumDao.delete(savedSum) }
    }
}