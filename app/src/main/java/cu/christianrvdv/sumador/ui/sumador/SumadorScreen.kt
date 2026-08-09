package cu.christianrvdv.sumador.ui.sumador

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cu.christianrvdv.sumador.R
import cu.christianrvdv.sumador.ui.settings.SettingsDialog
import cu.christianrvdv.sumador.ui.settings.SettingsViewModel
import cu.christianrvdv.sumador.ui.settings.CurrencySymbol
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

    // Sincronizar autoSave
    LaunchedEffect(settingsState.autoSave) {
        viewModel.setAutoSave(settingsState.autoSave)
    }

    // Notificar al ViewModel cuando cambie la moneda para cargar las cantidades correspondientes
    LaunchedEffect(settingsState.currencySymbol) {
        viewModel.setCurrency(settingsState.currencySymbol)
    }

    // Obtener denominaciones según la moneda actual
    val denominacionesActuales = remember(settingsState.currencySymbol) {
        getDenominations(settingsState.currencySymbol)
    }

    // Ordenar denominaciones según preferencia
    val denominacionesOrdenadas = remember(settingsState.sortAscending, settingsState.currencySymbol) {
        if (settingsState.sortAscending) denominacionesActuales.sorted() else denominacionesActuales.sortedDescending()
    }

    // Animación del total (escala + color) con reinicio automático
    var pulse by remember { mutableStateOf(false) }
    val totalScale by animateFloatAsState(
        targetValue = if (pulse) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "total_scale"
    )
    val totalColor by animateColorAsState(
        targetValue = if (pulse) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = tween(300),
        label = "total_color"
    )
    // Cuando el total cambia, activamos el pulso y lo desactivamos tras un breve retraso
    LaunchedEffect(state.total) {
        pulse = true
        delay(300) // duración de la animación
        pulse = false
    }

    // Visibilidad escalonada de filas (se reinicia al cambiar la lista de denominaciones)
    val rowVisibility = remember(denominacionesOrdenadas) {
        denominacionesOrdenadas.map { mutableStateOf(false) }
    }
    // Al cambiar la lista, reiniciamos las visibilidades
    LaunchedEffect(denominacionesOrdenadas) {
        denominacionesOrdenadas.forEachIndexed { index, _ ->
            delay(index * 60L)
            rowVisibility[index].value = true
        }
    }

    // Total formateado
    val totalFormateado = remember(state.total) {
        NumberFormat.getIntegerInstance().format(state.total)
    }
    // Conteo de billetes totales
    val totalBills = state.cantidades.values.sumOf { it.toIntOrNull() ?: 0 }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = stringResource(R.string.total_bills, totalBills),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (settingsState.confirmClear) {
                        showResetDialog = true
                    } else {
                        viewModel.resetear()
                    }
                },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_all))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Lista de billetes
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tarjeta del total con gradiente
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(totalScale),
                colors = CardDefaults.cardColors(containerColor = totalColor),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.total).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$totalFormateado ${settingsState.currencySymbol.symbol}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (totalBills > 0) {
                            Text(
                                text = stringResource(R.string.total_bills_detail, totalBills),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogos
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

    if (showSettingsDialog) {
        SettingsDialog(
            settingsState = settingsState,
            onDismiss = { showSettingsDialog = false },
            onThemeChange = { theme ->
                coroutineScope.launch { settingsViewModel.updateTheme(theme) }
            },
            onCurrencyChange = { currency ->
                coroutineScope.launch { settingsViewModel.updateCurrency(currency) }
            },
            onSortChange = { ascending ->
                coroutineScope.launch { settingsViewModel.updateSortOrder(ascending) }
            },
            onAutoSaveChange = { enabled ->
                coroutineScope.launch { settingsViewModel.updateAutoSave(enabled) }
            },
            onConfirmClearChange = { enabled ->
                coroutineScope.launch { settingsViewModel.updateConfirmClear(enabled) }
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
    var textFieldValue by remember(value) { mutableStateOf(value) }

    // Sincronizar con el estado externo (cuando se resetea, etc.)
    LaunchedEffect(value) {
        if (textFieldValue != value) {
            textFieldValue = value
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Money,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))

            Text(
                text = "$denomination $currencySymbol",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Botón decrementar
            IconButton(
                onClick = {
                    val current = textFieldValue.toIntOrNull() ?: 0
                    if (current > 0) {
                        val newVal = (current - 1).toString()
                        textFieldValue = newVal
                        onValueChange(newVal)
                    }
                },
                enabled = textFieldValue.toIntOrNull()?.let { it > 0 } ?: false,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrementar")
            }

            // Campo de texto
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newText: String ->
                    if (newText.all { it.isDigit() } && newText.length <= 5) {
                        textFieldValue = newText
                        onValueChange(newText)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .width(80.dp)
                    .padding(vertical = 4.dp),
                placeholder = { Text("0") },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    textAlign = TextAlign.Center
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Botón incrementar
            IconButton(
                onClick = {
                    val current = textFieldValue.toIntOrNull() ?: 0
                    val newVal = (current + 1).toString()
                    textFieldValue = newVal
                    onValueChange(newVal)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Incrementar")
            }
        }
    }
}