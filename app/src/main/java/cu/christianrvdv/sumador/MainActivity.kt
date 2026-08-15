package cu.christianrvdv.sumador

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

            // Estados para la actualización
            var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            var showCheckingDialog by remember { mutableStateOf(false) }
            var showDownloadStartedDialog by remember { mutableStateOf(false) }
            var showNoUpdateDialog by remember { mutableStateOf(false) }
            var showUpdateErrorDialog by remember { mutableStateOf(false) }

            // Verificar actualizaciones al inicio
            LaunchedEffect(Unit) {
                val info = updateManager.checkForUpdate()
                if (info != null) {
                    updateInfo = info
                    showUpdateDialog = true
                }
            }

            // Función para verificar manualmente (usada desde Settings)
            suspend fun checkForUpdatesManually() {
                showCheckingDialog = true
                try {
                    val info = updateManager.checkForUpdate()
                    showCheckingDialog = false
                    if (info != null) {
                        updateInfo = info
                        showUpdateDialog = true
                    } else {
                        showNoUpdateDialog = true
                    }
                } catch (e: Exception) {
                    showCheckingDialog = false
                    showUpdateErrorDialog = true
                }
            }

            // === DIÁLOGOS ===

            // 1. Confirmación de actualización
            if (showUpdateDialog && updateInfo != null) {
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = false },
                    title = { Text("Actualización disponible") },
                    text = { Text("Hay una nueva versión (${updateInfo!!.version}) disponible. ¿Deseas descargarla e instalarla?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val info = updateInfo!!
                                showUpdateDialog = false
                                // Se pasa la versión para que el Worker pueda guardar el APK con ese nombre
                                updateManager.startBackgroundDownload(info.downloadUrl, info.version)
                                showDownloadStartedDialog = true
                            }
                        ) {
                            Text("Actualizar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // 2. Descarga iniciada
            if (showDownloadStartedDialog) {
                AlertDialog(
                    onDismissRequest = { showDownloadStartedDialog = false },
                    title = { Text("Descarga iniciada") },
                    text = { Text("La actualización se está descargando en segundo plano. Recibirás una notificación cuando termine.") },
                    confirmButton = {
                        TextButton(onClick = { showDownloadStartedDialog = false }) {
                            Text("Aceptar")
                        }
                    }
                )
            }

            // 3. Buscando actualizaciones (manual)
            if (showCheckingDialog) {
                Dialog(onDismissRequest = { }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Column (
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text("Buscando actualizaciones...")
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // 4. No hay actualizaciones
            if (showNoUpdateDialog) {
                AlertDialog(
                    onDismissRequest = { showNoUpdateDialog = false },
                    title = { Text("Sin actualizaciones") },
                    text = { Text("Ya tienes la versión más reciente instalada.") },
                    confirmButton = {
                        TextButton(onClick = { showNoUpdateDialog = false }) {
                            Text("Aceptar")
                        }
                    }
                )
            }

            // 5. Error al buscar actualizaciones
            if (showUpdateErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showUpdateErrorDialog = false },
                    title = { Text("Error") },
                    text = { Text("No se pudo comprobar si hay actualizaciones. Verifica tu conexión a Internet.") },
                    confirmButton = {
                        TextButton(onClick = { showUpdateErrorDialog = false }) {
                            Text("Aceptar")
                        }
                    }
                )
            }

            // 6. (Opcional) Diálogo de progreso de descarga en primer plano (ya no se usa porque ahora es en segundo plano con notificación)
            // Lo eliminamos.

            key(settingsState.language) {
                SumadorTheme(
                    darkTheme = useDarkTheme,
                    dynamicColor = settingsState.theme == ThemeOption.SYSTEM
                ) {
                    NavHost(navController, startDestination = "sumador") {
                        composable("sumador") {
                            SumadorScreen(
                                settingsViewModel = settingsViewModel,
                                onNavigateToHistory = { navController.navigate("history") },
                                onCheckForUpdates = {
                                    // Lanzar la verificación manual
                                    lifecycleScope.launch {
                                        checkForUpdatesManually()
                                    }
                                }
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