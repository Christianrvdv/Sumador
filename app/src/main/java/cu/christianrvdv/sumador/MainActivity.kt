package cu.christianrvdv.sumador

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.lifecycleScope
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
import cu.christianrvdv.sumador.utils.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Verificar actualizaciones al inicio (no bloquea la UI)
        lifecycleScope.launch {
            checkForUpdate()
        }

        setContent {
            val context = LocalContext.current
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(context)
            )
            val settingsState by settingsViewModel.state.collectAsState()

            // Aplicar idioma
            LaunchedEffect(settingsState.language) {
                applyLanguage(settingsState.language)
            }

            // Mantener pantalla activa
            val view = LocalView.current
            LaunchedEffect(settingsState.keepScreenOn) {
                view.setKeepScreenOn(settingsState.keepScreenOn)
            }

            val useDarkTheme = when (settingsState.theme) {
                ThemeOption.DARK -> true
                ThemeOption.LIGHT -> false
                ThemeOption.SYSTEM -> null
            }

            val navController = rememberNavController()

            key(settingsState.language) {
                SumadorTheme(
                    darkTheme = useDarkTheme,
                    dynamicColor = settingsState.theme == ThemeOption.SYSTEM
                ) {
                    NavHost(navController, startDestination = "sumador") {
                        composable("sumador") {
                            SumadorScreen(
                                settingsViewModel = settingsViewModel,
                                onNavigateToHistory = { navController.navigate("history") }
                            )
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

    private suspend fun checkForUpdate() {
        val updateManager = UpdateManager(this)
        val updateInfo = updateManager.checkForUpdate()
        if (updateInfo != null) {
            // Mostrar diálogo en el hilo principal
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Actualización disponible")
                    .setMessage("Hay una nueva versión (${updateInfo.version}) disponible. ¿Deseas descargarla e instalarla?")
                    .setPositiveButton("Actualizar") { _, _ ->
                        lifecycleScope.launch {
                            val success = updateManager.downloadAndInstall(updateInfo.downloadUrl)
                            if (!success) {
                                // Mostrar error
                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle("Error")
                                    .setMessage("No se pudo descargar la actualización. Inténtalo más tarde.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun applyLanguage(language: LanguageOption) {
        val desiredLocale = when (language) {
            LanguageOption.ENGLISH -> Locale.ENGLISH
            LanguageOption.SPANISH -> Locale("es")
            LanguageOption.SYSTEM -> Locale.getDefault()
        }

        val currentLocale = resources.configuration.locales[0] ?: Locale.getDefault()
        if (currentLocale == desiredLocale) {
            Log.d("MainActivity", "Idioma ya aplicado: $desiredLocale")
            return
        }

        Log.d("MainActivity", "Aplicando idioma: $language -> locale: $desiredLocale")
        if (language != LanguageOption.SYSTEM) {
            Locale.setDefault(desiredLocale)
        }

        val config = Configuration(resources.configuration)
        config.setLocale(desiredLocale)
        resources.updateConfiguration(config, resources.displayMetrics)

        recreate()
    }
}