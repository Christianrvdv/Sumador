package cu.christianrvdv.sumador.ui.settings

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(private val context: Context) : ViewModel() {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val CURRENCY = stringPreferencesKey("currency")
        val SORT_ASC = booleanPreferencesKey("sort_asc")
    }

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data
                .catch { exception ->
                    Log.e("SettingsViewModel", "Error reading settings", exception)
                    // Emitir un estado por defecto en caso de error
                    emit(emptyPreferences())
                }
                .collect { prefs ->
                    try {
                        val themeStr = prefs[Keys.THEME] ?: "SYSTEM"
                        val theme = ThemeOption.valueOf(themeStr)
                        val currencyStr = prefs[Keys.CURRENCY] ?: "PESO"
                        val currency = CurrencySymbol.valueOf(currencyStr)
                        val sortAsc = prefs[Keys.SORT_ASC] ?: true
                        _state.value = SettingsState(theme, currency, sortAsc)
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error parsing settings", e)
                        // Mantener el estado por defecto
                    }
                }
        }
    }

    suspend fun updateTheme(theme: ThemeOption) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.THEME] = theme.name
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving theme", e)
        }
    }

    suspend fun updateCurrency(currency: CurrencySymbol) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.CURRENCY] = currency.name
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving currency", e)
        }
    }

    suspend fun updateSortOrder(ascending: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.SORT_ASC] = ascending
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving sort order", e)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(context) as T
            }
        }
    }
}