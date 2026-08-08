package cu.christianrvdv.sumador.ui.sumador

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cu.christianrvdv.sumador.ui.settings.SettingsDialog
import cu.christianrvdv.sumador.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

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

    val denominacionesOrdenadas = remember(settingsState.sortAscending) {
        if (settingsState.sortAscending) {
            denominaciones.sorted()
        } else {
            denominaciones.sortedDescending()
        }
    }

    val totalScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "total_scale"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Sumador de Efectivo") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuraciones")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            denominacionesOrdenadas.forEach { denom ->
                BillInputRow(
                    denomination = denom,
                    value = state.cantidades[denom] ?: "",
                    onValueChange = { newValue ->
                        viewModel.updateCantidad(denom, newValue)
                    },
                    currencySymbol = settingsState.currencySymbol.symbol
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            FilledTonalButton(
                onClick = { viewModel.resetear() },
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
                Text("Limpiar todo", style = MaterialTheme.typography.titleLarge)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(totalScale),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${state.total} ${settingsState.currencySymbol.symbol}",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

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
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Money,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))

            Text(
                text = "Billetes de $denomination$currencySymbol",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(100.dp),
                placeholder = { Text("0") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
        }
    }
}