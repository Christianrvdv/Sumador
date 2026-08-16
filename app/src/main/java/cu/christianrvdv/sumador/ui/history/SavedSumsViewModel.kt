// ui/history/SavedSumsViewModel.kt
package cu.christianrvdv.sumador.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import cu.christianrvdv.sumador.data.database.SavedSumDao
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortBy { DATE, NAME }
enum class SortDirection { ASC, DESC }

data class FilterState(
    val name: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val totalMin: Long? = null,
    val totalMax: Long? = null,
    val sortBy: SortBy = SortBy.DATE,
    val sortDirection: SortDirection = SortDirection.DESC
)

@HiltViewModel
class SavedSumsViewModel @Inject constructor(
    private val savedSumDao: SavedSumDao
) : ViewModel() {

    // Flujos internos para cada filtro
    private val _filterName = MutableStateFlow<String?>(null)
    private val _filterDateFrom = MutableStateFlow<Long?>(null)
    private val _filterDateTo = MutableStateFlow<Long?>(null)
    private val _filterTotalMin = MutableStateFlow<Long?>(null)
    private val _filterTotalMax = MutableStateFlow<Long?>(null)
    private val _filterSortBy = MutableStateFlow(SortBy.DATE)
    private val _filterSortDirection = MutableStateFlow(SortDirection.DESC)

    // Estado combinado para la UI
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // Lista filtrada con orden dinámico
    val allSavedSums: Flow<List<SavedSumEntity>> = combine(
        listOf(
            _filterName,
            _filterDateFrom,
            _filterDateTo,
            _filterTotalMin,
            _filterTotalMax,
            _filterSortBy,
            _filterSortDirection
        )
    ) { values ->
        val name = values[0] as? String?
        val dateFrom = values[1] as? Long?
        val dateTo = values[2] as? Long?
        val totalMin = values[3] as? Long?
        val totalMax = values[4] as? Long?
        val sortBy = values[5] as SortBy
        val sortDirection = values[6] as SortDirection

        // Construir la consulta SQL con ORDER BY dinámico
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()

        if (!name.isNullOrEmpty()) {
            conditions.add("name LIKE ?")
            args.add("%$name%")
        }
        dateFrom?.let {
            conditions.add("timestamp >= ?")
            args.add(it)
        }
        dateTo?.let {
            conditions.add("timestamp <= ?")
            args.add(it)
        }
        totalMin?.let {
            conditions.add("total >= ?")
            args.add(it)
        }
        totalMax?.let {
            conditions.add("total <= ?")
            args.add(it)
        }

        val whereClause = if (conditions.isEmpty()) "" else "WHERE " + conditions.joinToString(" AND ")
        val orderByColumn = when (sortBy) {
            SortBy.DATE -> "timestamp"
            SortBy.NAME -> "name"
        }
        val orderDir = if (sortDirection == SortDirection.ASC) "ASC" else "DESC"
        val sql = "SELECT * FROM saved_sums $whereClause ORDER BY $orderByColumn $orderDir"

        val query = SimpleSQLiteQuery(sql, args.toTypedArray())
        savedSumDao.getFilteredOrdered(query)
    }.flatMapLatest { it }

    // Actualizar filtros (incluyendo orden)
    fun setFilter(
        name: String?,
        dateFrom: Long?,
        dateTo: Long?,
        totalMin: Long?,
        totalMax: Long?,
        sortBy: SortBy = _filterSortBy.value,
        sortDirection: SortDirection = _filterSortDirection.value
    ) {
        _filterName.value = name
        _filterDateFrom.value = dateFrom
        _filterDateTo.value = dateTo
        _filterTotalMin.value = totalMin
        _filterTotalMax.value = totalMax
        _filterSortBy.value = sortBy
        _filterSortDirection.value = sortDirection
        _filterState.value = FilterState(name, dateFrom, dateTo, totalMin, totalMax, sortBy, sortDirection)
    }

    fun clearFilters() {
        setFilter(null, null, null, null, null, SortBy.DATE, SortDirection.DESC)
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

    // NUEVO: Eliminar todo el historial
    fun deleteAll() {
        viewModelScope.launch { savedSumDao.deleteAll() }
    }
}