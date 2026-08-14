// ui/history/SavedSumsViewModel.kt
package cu.christianrvdv.sumador.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.christianrvdv.sumador.data.database.SavedSumDao
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedSumsViewModel @Inject constructor(
    private val savedSumDao: SavedSumDao
) : ViewModel() {

    // Estado de los filtros
    private val _filterName = MutableStateFlow<String?>(null)
    private val _filterDateFrom = MutableStateFlow<Long?>(null)
    private val _filterDateTo = MutableStateFlow<Long?>(null)
    private val _filterTotalMin = MutableStateFlow<Long?>(null)
    private val _filterTotalMax = MutableStateFlow<Long?>(null)

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