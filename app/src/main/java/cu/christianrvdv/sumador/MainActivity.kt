// MainActivity.kt
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cu.christianrvdv.sumador.ui.history.SavedSumsScreen
import cu.christianrvdv.sumador.ui.settings.LanguageOption
import cu.christianrvdv.sumador.ui.settings.SettingsViewModel
import cu.christianrvdv.sumador.ui.settings.ThemeOption
import cu.christianrvdv.sumador.ui.sumador.SumadorScreen
import cu.christianrvdv.sumador.ui.theme.SumadorTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
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

            val navController = rememberNavController()

            // Forzar recomposición completa al cambiar el idioma
            key(settingsState.language) {
                SumadorTheme(
                    darkTheme = useDarkTheme,
                    dynamicColor = settingsState.theme == ThemeOption.SYSTEM
                ) {
                    NavHost(navController, startDestination = "sumador") {
                        composable("sumador") {
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                SumadorScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    settingsViewModel = settingsViewModel,
                                    onNavigateToHistory = { navController.navigate("history") }
                                )
                            }
                        }
                        composable("history") {
                            SavedSumsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
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