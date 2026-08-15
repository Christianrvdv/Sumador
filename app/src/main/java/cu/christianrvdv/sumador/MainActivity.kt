package cu.christianrvdv.sumador

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import cu.christianrvdv.sumador.utils.DownloadState
import cu.christianrvdv.sumador.utils.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val updateManager by lazy { UpdateManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

            // Estado para la actualización
            var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
            val downloadState by updateManager.downloadState.collectAsState()

            // Verificar actualizaciones al inicio
            LaunchedEffect(Unit) {
                val info = updateManager.checkForUpdate()
                if (info != null) {
                    updateInfo = info
                }
            }

            // Diálogo de confirmación de actualización
            if (updateInfo != null) {
                AlertDialog(
                    onDismissRequest = { updateInfo = null },
                    title = { Text("Actualización disponible") },
                    text = { Text("Hay una nueva versión (${updateInfo!!.version}) disponible. ¿Deseas descargarla e instalarla?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val info = updateInfo!!
                                updateInfo = null
                                lifecycleScope.launch {
                                    updateManager.downloadAndInstall(info.downloadUrl)
                                }
                            }
                        ) {
                            Text("Actualizar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { updateInfo = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Diálogo de progreso de descarga
            when (downloadState) {
                is DownloadState.Downloading -> {
                    Dialog(onDismissRequest = { /* No permitir cerrar */ }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Descargando actualización...",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = (downloadState as DownloadState.Downloading).progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${((downloadState as DownloadState.Downloading).progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                is DownloadState.Error -> {
                    // Mostrar diálogo de error y resetear estado
                    AlertDialog(
                        onDismissRequest = {
                            updateManager.resetState()
                        },
                        title = { Text("Error") },
                        text = { Text((downloadState as DownloadState.Error).message) },
                        confirmButton = {
                            TextButton(onClick = {
                                updateManager.resetState()
                            }) {
                                Text("OK")
                            }
                        }
                    )
                }
                else -> { /* Idle o Completed: no mostrar diálogo */ }
            }

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