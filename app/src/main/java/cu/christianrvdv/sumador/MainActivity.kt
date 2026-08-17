package cu.christianrvdv.sumador

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cu.christianrvdv.sumador.ui.history.SavedSumsScreen
import cu.christianrvdv.sumador.ui.manage_denominations.ManageDenominationsScreen
import cu.christianrvdv.sumador.ui.settings.LanguageOption
import cu.christianrvdv.sumador.ui.settings.SettingsViewModel
import cu.christianrvdv.sumador.ui.settings.ThemeOption
import cu.christianrvdv.sumador.ui.sumador.SumadorScreen
import cu.christianrvdv.sumador.ui.theme.SumadorTheme
import cu.christianrvdv.sumador.utils.DownloadWorker
import cu.christianrvdv.sumador.utils.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val updateManager by lazy { UpdateManager(this) }

    // Lanzador para solicitar permiso de notificaciones (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Permiso de notificaciones concedido")
        } else {
            Log.d("MainActivity", "Permiso de notificaciones denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Ya tiene permiso
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        setContent {
            val context = LocalContext.current
            val settingsViewModel: SettingsViewModel = hiltViewModel()
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
            var showNetworkErrorDialog by remember { mutableStateOf(false) }

            // Flag para evitar ejecutar la verificación automática más de una vez
            var autoCheckDone by remember { mutableStateOf(false) }

            // Verificar actualizaciones al inicio (solo una vez)
            LaunchedEffect(Unit) {
                if (!autoCheckDone) {
                    autoCheckDone = true
                    val result = updateManager.checkForUpdate()
                    when (result) {
                        is UpdateManager.UpdateResult.Success -> {
                            // Solo mostramos diálogo si hay actualización disponible
                            updateInfo = result.info
                            showUpdateDialog = true
                        }
                        // Los demás casos son silenciosos al inicio:
                        UpdateManager.UpdateResult.NoUpdate -> {
                            // No mostrar nada
                            Log.d("MainActivity", "Sin actualizaciones (silencioso)")
                        }
                        UpdateManager.UpdateResult.NetworkError -> {
                            Log.d("MainActivity", "Error de red (silencioso)")
                        }
                        is UpdateManager.UpdateResult.Error -> {
                            Log.e("MainActivity", "Error al comprobar actualizaciones (silencioso)", result.throwable)
                        }
                    }
                }
            }

            // Función para verificar manualmente (usada desde Settings)
            suspend fun checkForUpdatesManually() {
                showCheckingDialog = true
                try {
                    val result = updateManager.checkForUpdate()
                    showCheckingDialog = false
                    when (result) {
                        is UpdateManager.UpdateResult.Success -> {
                            updateInfo = result.info
                            showUpdateDialog = true
                        }
                        UpdateManager.UpdateResult.NoUpdate -> {
                            showNoUpdateDialog = true
                        }
                        UpdateManager.UpdateResult.NetworkError -> {
                            showNetworkErrorDialog = true
                        }
                        is UpdateManager.UpdateResult.Error -> {
                            showUpdateErrorDialog = true
                        }
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
                    title = { Text(stringResource(R.string.update_dialog_available_title)) },
                    text = {
                        val info = updateInfo!!
                        Text(
                            if (info.alreadyDownloaded) {
                                stringResource(R.string.update_dialog_already_downloaded_message, info.version)
                            } else {
                                stringResource(R.string.update_dialog_available_message, info.version)
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val info = updateInfo!!
                                showUpdateDialog = false
                                // Guardar la actualización pendiente en Application
                                (application as SumadorApplication).pendingUpdate = info
                                // Iniciar descarga (o instalación directa si ya está descargado)
                                updateManager.startBackgroundDownload(info.downloadUrl, info.version)
                                showDownloadStartedDialog = true
                            }
                        ) {
                            Text(stringResource(R.string.update_dialog_update_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text(stringResource(R.string.update_dialog_cancel_button))
                        }
                    }
                )
            }

            // 2. Descarga iniciada
            if (showDownloadStartedDialog) {
                AlertDialog(
                    onDismissRequest = { showDownloadStartedDialog = false },
                    title = { Text(stringResource(R.string.update_dialog_download_started_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.update_dialog_download_started_message))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.update_dialog_download_started_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDownloadStartedDialog = false }) {
                            Text(stringResource(R.string.update_dialog_accept_button))
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
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.update_dialog_checking_title))
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
                    title = { Text(stringResource(R.string.update_dialog_no_update_title)) },
                    text = { Text(stringResource(R.string.update_dialog_no_update_message)) },
                    confirmButton = {
                        TextButton(onClick = { showNoUpdateDialog = false }) {
                            Text(stringResource(R.string.update_dialog_accept_button))
                        }
                    }
                )
            }

            // 5. Error al buscar actualizaciones (genérico)
            if (showUpdateErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showUpdateErrorDialog = false },
                    title = { Text(stringResource(R.string.update_dialog_error_title)) },
                    text = { Text(stringResource(R.string.update_dialog_error_message)) },
                    confirmButton = {
                        TextButton(onClick = { showUpdateErrorDialog = false }) {
                            Text(stringResource(R.string.update_dialog_accept_button))
                        }
                    }
                )
            }

            // 6. Error de red
            if (showNetworkErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showNetworkErrorDialog = false },
                    title = { Text(stringResource(R.string.update_dialog_network_error_title)) },
                    text = { Text(stringResource(R.string.update_dialog_network_error_message)) },
                    confirmButton = {
                        TextButton(onClick = { showNetworkErrorDialog = false }) {
                            Text(stringResource(R.string.update_dialog_accept_button))
                        }
                    }
                )
            }

            key(settingsState.language) {
                SumadorTheme(
                    darkTheme = useDarkTheme,
                    dynamicColor = settingsState.theme == ThemeOption.SYSTEM
                ) {
                    NavHost(navController, startDestination = "sumador") {
                        composable("sumador") {
                            SumadorScreen(
                                navController = navController,
                                settingsViewModel = settingsViewModel,
                                onNavigateToHistory = { navController.navigate("history") },
                                onCheckForUpdates = {
                                    lifecycleScope.launch {
                                        checkForUpdatesManually()
                                    }
                                },
                                onNavigateToManageDenominations = {
                                    val currency = settingsState.currencySymbol.name
                                    navController.navigate("manage_denominations/$currency")
                                }
                            )
                        }
                        composable("history") {
                            SavedSumsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "manage_denominations/{currency}",
                            arguments = listOf(navArgument("currency") { defaultValue = "PESO" })
                        ) {
                            ManageDenominationsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Verificar si hay una actualización pendiente y el permiso de instalación está concedido
        val pending = (application as SumadorApplication).pendingUpdate
        if (pending != null) {
            val permisoConcedido = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                packageManager.canRequestPackageInstalls()
            } else {
                true
            }

            if (permisoConcedido) {
                // Usamos el método estático de DownloadWorker para obtener el archivo y validarlo
                val apkFile = DownloadWorker.getApkFile(this, pending.version)
                if (apkFile.exists() && DownloadWorker.isApkValid(this, apkFile)) {
                    Log.d("MainActivity", "APK válido encontrado en caché, iniciando instalación automática")
                    // Iniciamos el Worker que instalará el APK (si ya existe, lo hará directamente)
                    updateManager.startBackgroundDownload(pending.downloadUrl, pending.version)
                    (application as SumadorApplication).pendingUpdate = null
                } else {
                    // Si existe pero es inválido, lo eliminamos
                    if (apkFile.exists()) {
                        apkFile.delete()
                        Log.d("MainActivity", "APK corrupto eliminado, se descargará de nuevo")
                    } else {
                        Log.d("MainActivity", "APK no encontrado en caché, se descargará de nuevo")
                    }
                    // Reiniciamos la descarga (el Worker se encargará)
                    updateManager.startBackgroundDownload(pending.downloadUrl, pending.version)
                    // No borramos pendingUpdate para que siga pendiente
                }
            } else {
                Log.d("MainActivity", "Permiso de instalación no concedido, esperando...")
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