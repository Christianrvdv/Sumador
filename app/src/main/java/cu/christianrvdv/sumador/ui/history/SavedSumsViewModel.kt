// ui/history/SavedSumsViewModel.kt
package cu.christianrvdv.sumador.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.christianrvdv.sumador.data.database.SavedSumDao
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedSumsViewModel @Inject constructor(
    private val savedSumDao: SavedSumDao
) : ViewModel() {

    val allSavedSums: Flow<List<SavedSumEntity>> = savedSumDao.getAll()

    fun insert(savedSum: SavedSumEntity) {
        viewModelScope.launch {
            savedSumDao.insert(savedSum)
        }
    }

    fun delete(savedSum: SavedSumEntity) {
        viewModelScope.launch {
            savedSumDao.delete(savedSum)
        }
    }
}