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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cu.christianrvdv.sumador.ui.settings.SettingsDialog
import cu.christianrvdv.sumador.ui.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SumadorScreen(
    modifier: Modifier = Modifier,
    viewModel: SumadorViewModel = viewModel(),
    settingsViewModel: SettingsViewModel
) {
    val state by viewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

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

    // Formateo del total con separadores de miles
    val totalFormateado = remember(state.total) {
        NumberFormat.getIntegerInstance().format(state.total)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sumador de Efectivo") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuraciones"
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
            // Filas de billetes con animación escalonada
            // Dentro del forEachIndexed en la Column
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

            // Botón de limpiar con confirmación
            FilledTonalButton(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar")
                Spacer(Modifier.width(10.dp))
                Text("Limpiar todo", style = MaterialTheme.typography.titleMedium)
            }

            // Tarjeta del total con animación de pulso
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
                        text = "TOTAL",
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

    // Diálogo de confirmación para reiniciar
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reiniciar sumador") },
            text = { Text("¿Estás seguro de que quieres borrar todas las cantidades?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetear()
                        showResetDialog = false
                    }
                ) {
                    Text("Sí, limpiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
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
                contentDescription = "Billete",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))

            Text(
                text = "Billetes de $denomination $currencySymbol",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = value,
                onValueChange = { newText ->
                    // Solo dígitos, máximo 5 caracteres
                    if (newText.all { it.isDigit() } && newText.length <= 5) {
                        onValueChange(newText)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(110.dp),
                placeholder = { Text("0") },
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