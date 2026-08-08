package cu.christianrvdv.sumador.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
                .map { prefs ->
                    val themeStr = prefs[Keys.THEME] ?: "SYSTEM"
                    val theme = ThemeOption.valueOf(themeStr)
                    val currencyStr = prefs[Keys.CURRENCY] ?: "PESO"
                    val currency = CurrencySymbol.valueOf(currencyStr)
                    val sortAsc = prefs[Keys.SORT_ASC] ?: true
                    SettingsState(theme, currency, sortAsc)
                }
                .collect { newState ->
                    _state.value = newState
                }
        }
    }

    suspend fun updateTheme(theme: ThemeOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme.name
        }
    }

    suspend fun updateCurrency(currency: CurrencySymbol) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENCY] = currency.name
        }
    }

    suspend fun updateSortOrder(ascending: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SORT_ASC] = ascending
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