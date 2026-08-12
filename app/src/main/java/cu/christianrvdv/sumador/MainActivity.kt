package cu.christianrvdv.sumador

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
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
import cu.christianrvdv.sumador.ui.settings.LanguageOption
import cu.christianrvdv.sumador.ui.settings.SettingsViewModel
import cu.christianrvdv.sumador.ui.settings.ThemeOption
import cu.christianrvdv.sumador.ui.sumador.SumadorScreen
import cu.christianrvdv.sumador.ui.theme.SumadorTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(context)
            )
            val settingsState by settingsViewModel.state.collectAsState()

            // Aplicar el idioma cada vez que cambie en el estado
            LaunchedEffect(settingsState.language) {
                applyLanguage(settingsState.language)
            }

            val useDarkTheme = when (settingsState.theme) {
                ThemeOption.DARK -> true
                ThemeOption.LIGHT -> false
                ThemeOption.SYSTEM -> null
            }

            // Forzar recomposición completa al cambiar el idioma (opcional, pero lo mantenemos)
            key(settingsState.language) {
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
    }

    /**
     * Aplica el idioma seleccionado a la configuración de recursos de la app.
     * Si el idioma cambia, recrea la actividad para que los cambios surtan efecto.
     */
    private fun applyLanguage(language: LanguageOption) {
        val desiredLocale = when (language) {
            LanguageOption.ENGLISH -> Locale.ENGLISH
            LanguageOption.SPANISH -> Locale("es")
            LanguageOption.SYSTEM -> Locale.getDefault()
        }

        // Obtener el locale actual de la configuración de recursos
        val currentLocale = resources.configuration.locales[0] ?: Locale.getDefault()

        // Si el locale deseado ya está aplicado, no hacer nada
        if (currentLocale == desiredLocale) {
            Log.d("MainActivity", "Idioma ya aplicado: $desiredLocale")
            return
        }

        Log.d("MainActivity", "Aplicando idioma: $language -> locale: $desiredLocale")

        // Establecer el locale por defecto (para formateo, etc.)
        if (language != LanguageOption.SYSTEM) {
            Locale.setDefault(desiredLocale)
        }

        // Actualizar la configuración de recursos
        val config = Configuration(resources.configuration)
        config.setLocale(desiredLocale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Forzar el recreado de la actividad para que todos los recursos se recarguen
        recreate()
    }
}