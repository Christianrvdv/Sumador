package cu.christianrvdv.sumador.ui.sumador

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cu.christianrvdv.sumador.R
import cu.christianrvdv.sumador.ui.settings.SettingsDialog
import cu.christianrvdv.sumador.ui.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SumadorScreen(
    modifier: Modifier = Modifier,
    viewModel: SumadorViewModel = viewModel(
        factory = SumadorViewModel.provideFactory(LocalContext.current)
    ),
    settingsViewModel: SettingsViewModel
) {
    val state by viewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Actualizar la bandera de autoSave en el ViewModel cuando cambie en settings
    LaunchedEffect(settingsState.autoSave) {
        viewModel.setAutoSave(settingsState.autoSave)
    }

    // Ordenar denominaciones según configuración
    val denominacionesOrdenadas = remember(settingsState.sortAscending) {
        if (settingsState.sortAscending) {
            denominaciones.sorted()
        } else {
            denominaciones.sortedDescending()
        }
    }

    // Animación de pulso en el total cuando cambia
    var pulse by remember { mutableStateOf(false) }
    val totalScale by animateFloatAsState(
        targetValue = if (pulse) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { pulse = false },
        label = "total_pulse"
    )
    LaunchedEffect(state.total) {
        pulse = true
    }

    // Estados de visibilidad para aparición escalonada de las filas
    val rowVisibility = remember { denominacionesOrdenadas.map { mutableStateOf(false) } }
    denominacionesOrdenadas.forEachIndexed { index, _ ->
        LaunchedEffect(index) {
            delay(index * 80L)
            rowVisibility[index].value = true
        }
    }

    val totalFormateado = remember(state.total) {
        NumberFormat.getIntegerInstance().format(state.total)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            denominacionesOrdenadas.forEachIndexed { index, denom ->
                AnimatedVisibility(
                    visible = rowVisibility[index].value,
                    enter = fadeIn(animationSpec = tween(400)) +
                            slideInVertically(animationSpec = tween(400)) { it / 2 },
                    exit = fadeOut(animationSpec = tween(400)) +
                            slideOutVertically(animationSpec = tween(400)) { it / 2 }
                ) {
                    BillInputRow(
                        denomination = denom,
                        value = state.cantidades[denom] ?: "",
                        onValueChange = { newValue ->
                            viewModel.updateCantidad(denom, newValue)
                        },
                        currencySymbol = settingsState.currencySymbol.symbol
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Botón de limpiar con confirmación condicional
            FilledTonalButton(
                onClick = {
                    if (settingsState.confirmClear) {
                        showResetDialog = true
                    } else {
                        viewModel.resetear()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_all))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.clear_all), style = MaterialTheme.typography.titleMedium)
            }

            // Tarjeta del total
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(totalScale),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.total),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$totalFormateado ${settingsState.currencySymbol.symbol}",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    // Diálogo de confirmación para reiniciar (solo se muestra si confirmClear es true y se dispara)
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.clear_confirmation_title)) },
            text = { Text(stringResource(R.string.clear_confirmation_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetear()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Diálogo de configuración
    if (showSettingsDialog) {
        SettingsDialog(
            settingsState = settingsState,
            onDismiss = { showSettingsDialog = false },
            onThemeChange = { theme ->
                coroutineScope.launch {
                    settingsViewModel.updateTheme(theme)
                }
            },
            onCurrencyChange = { currency ->
                coroutineScope.launch {
                    settingsViewModel.updateCurrency(currency)
                }
            },
            onSortChange = { ascending ->
                coroutineScope.launch {
                    settingsViewModel.updateSortOrder(ascending)
                }
            },
            onAutoSaveChange = { enabled ->
                coroutineScope.launch {
                    settingsViewModel.updateAutoSave(enabled)
                }
            },
            onConfirmClearChange = { enabled ->
                coroutineScope.launch {
                    settingsViewModel.updateConfirmClear(enabled)
                }
            }
        )
    }
}

@Composable
fun BillInputRow(
    denomination: Int,
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Money,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))

            Text(
                text = "${stringResource(R.string.bill_of)} $denomination $currencySymbol",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = value,
                onValueChange = { newText ->
                    if (newText.all { it.isDigit() } && newText.length <= 5) {
                        onValueChange(newText)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(110.dp),
                placeholder = { Text(stringResource(R.string.placeholder_zero)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}