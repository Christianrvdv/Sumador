package cu.christianrvdv.sumador

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import cu.christianrvdv.sumador.ui.settings.LanguageOption
import cu.christianrvdv.sumador.ui.settings.SettingsViewModel
import cu.christianrvdv.sumador.ui.settings.ThemeOption
import cu.christianrvdv.sumador.ui.sumador.SumadorScreen
import cu.christianrvdv.sumador.ui.theme.SumadorTheme
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    private var currentLanguage: LanguageOption = LanguageOption.SYSTEM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Leer la preferencia de idioma guardada (síncrono para aplicarla antes de setContent)
        val savedLanguage = runBlocking {
            val key = stringPreferencesKey("language")
            val prefs = dataStore.data.first()
            val langStr = prefs[key] ?: "SYSTEM"
            LanguageOption.valueOf(langStr)
        }
        currentLanguage = savedLanguage
        applyLanguage(savedLanguage)

        setContent {
            val context = LocalContext.current
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(context)
            )
            val settingsState by settingsViewModel.state.collectAsState()

            // Observar cambios en el idioma desde el ViewModel
            LaunchedEffect(settingsState.language) {
                val newLang = settingsState.language
                if (newLang != currentLanguage) {
                    currentLanguage = newLang
                    applyLanguage(newLang)
                    // Forzar recreación de la actividad para aplicar el nuevo locale
                    recreate()
                }
            }

            val useDarkTheme = when (settingsState.theme) {
                ThemeOption.DARK -> true
                ThemeOption.LIGHT -> false
                ThemeOption.SYSTEM -> null
            }

            SumadorTheme(
                darkTheme = useDarkTheme,
                dynamicColor = settingsState.theme == ThemeOption.SYSTEM
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SumadorScreen(
                        modifier = Modifier.padding(innerPadding),
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    /**
     * Aplica el idioma seleccionado a la configuración de recursos de la app.
     */
    private fun applyLanguage(language: LanguageOption) {
        val locale = when (language) {
            LanguageOption.ENGLISH -> Locale.ENGLISH
            LanguageOption.SPANISH -> Locale("es")
            LanguageOption.SYSTEM -> Locale.getDefault() // usa el del sistema
        }
        // Si es SYSTEM, usamos el locale del sistema sin modificar
        if (language != LanguageOption.SYSTEM) {
            Locale.setDefault(locale)
        }
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}