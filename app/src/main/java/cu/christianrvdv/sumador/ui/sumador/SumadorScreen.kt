// SumadorScreen.kt
package cu.christianrvdv.sumador.ui.sumador

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cu.christianrvdv.sumador.R
import cu.christianrvdv.sumador.data.database.Converters
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import cu.christianrvdv.sumador.ui.settings.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SumadorScreen(
    modifier: Modifier = Modifier,
    viewModel: SumadorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }

    // Sincronizar autoSave
    LaunchedEffect(settingsState.autoSave) {
        viewModel.setAutoSave(settingsState.autoSave)
    }

    // Sincronizar moneda
    LaunchedEffect(settingsState.currencySymbol) {
        viewModel.setCurrency(settingsState.currencySymbol)
    }

    val denominacionesActuales = remember(settingsState.currencySymbol) {
        getDenominations(settingsState.currencySymbol)
    }

    val denominacionesOrdenadas =
        remember(settingsState.sortAscending, settingsState.currencySymbol) {
            if (settingsState.sortAscending) denominacionesActuales.sorted() else denominacionesActuales.sortedDescending()
        }

    val totalFormateado = remember(state.total) {
        NumberFormat.getIntegerInstance().format(state.total)
    }
    val totalBills = state.cantidades.values.sumOf { it.toIntOrNull() ?: 0 }

    // Animación del total
    val totalScale by animateFloatAsState(
        targetValue = if (state.total > 0) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "total_scale_bottom"
    )

    val isEmpty = state.cantidades.values.all { it.toIntOrNull() == 0 || it.isEmpty() }
    val hasBills = state.cantidades.values.any { it.toIntOrNull() ?: 0 > 0 }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    // Botón Historial
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Historial"
                        )
                    }
                    // Botón Acerca de
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.content_description_about)
                        )
                    }
                    // Botón Configuración
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
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .scale(totalScale)
                    ) {
                        Text(
                            text = stringResource(R.string.total).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.5.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$totalFormateado ",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = settingsState.currencySymbol.symbol,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (totalBills > 0) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.total_bills_short, totalBills),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            if (settingsState.confirmClear) {
                                showResetDialog = true
                            } else {
                                viewModel.resetear()
                            }
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.clear_all)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (hasBills) {
                FloatingActionButton(
                    onClick = { showSaveDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save_sum))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEmpty) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.empty_state_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.empty_state_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            denominacionesOrdenadas.forEachIndexed { index, denom ->
                val enterTransition = fadeIn(animationSpec = tween(300, delayMillis = index * 40)) +
                        slideInVertically(
                            animationSpec = tween(
                                300,
                                delayMillis = index * 40
                            )
                        ) { it / 3 }

                AnimatedVisibility(
                    visible = true,
                    enter = enterTransition,
                    exit = fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(animationSpec = tween(200)) { it / 3 }
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

            Spacer(Modifier.height(16.dp))
        }
    }

    // Diálogo de confirmación de reset
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

    // Diálogo de guardar suma
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                saveName = ""
            },
            title = { Text(stringResource(R.string.save_sum)) },
            text = {
                Column {
                    Text("Introduce un nombre para esta suma:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        placeholder = { Text(stringResource(R.string.save_sum_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalName = if (saveName.isNotBlank()) saveName else {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            dateFormat.format(Date())
                        }
                        // Construir mapa de denominaciones con cantidades (Int)
                        val denominationsMap = state.cantidades.mapValues { it.value.toIntOrNull() ?: 0 }
                        viewModel.saveCurrentSum(
                            name = finalName,
                            total = state.total,
                            denominationsMap = denominationsMap
                        )
                        showSaveDialog = false
                        saveName = ""
                    }
                ) {
                    Text(stringResource(R.string.save_sum))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    saveName = ""
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Diálogo de configuración
    if (showSettingsDialog) {
        SettingsBottomSheet(
            settingsState = settingsState,
            onDismiss = { showSettingsDialog = false },
            onThemeChange = { theme: ThemeOption ->
                coroutineScope.launch { settingsViewModel.updateTheme(theme) }
            },
            onCurrencyChange = { currency: CurrencySymbol ->
                coroutineScope.launch { settingsViewModel.updateCurrency(currency) }
            },
            onSortChange = { ascending: Boolean ->
                coroutineScope.launch { settingsViewModel.updateSortOrder(ascending) }
            },
            onAutoSaveChange = { enabled: Boolean ->
                coroutineScope.launch { settingsViewModel.updateAutoSave(enabled) }
            },
            onConfirmClearChange = { enabled: Boolean ->
                coroutineScope.launch { settingsViewModel.updateConfirmClear(enabled) }
            },
            onLanguageChange = { language: LanguageOption ->
                coroutineScope.launch { settingsViewModel.updateLanguage(language) }
            },
            onAboutClick = {
                showSettingsDialog = false
                showAboutDialog = true
            }
        )
    }

    // Diálogo Acerca de
    if (showAboutDialog) {
        AboutBottomSheet(
            onDismiss = { showAboutDialog = false }
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

    LaunchedEffect(value) {
        if (textFieldValue != value) {
            textFieldValue = value
        }
    }

    val billCount = textFieldValue.toIntOrNull() ?: 0

    val scale by animateFloatAsState(
        targetValue = if (billCount > 0) 1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (billCount > 0)
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Money,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = denomination.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        if (billCount > 0) {
                            val newVal = (billCount - 1).toString()
                            textFieldValue = newVal
                            onValueChange(newVal)
                        }
                    },
                    enabled = billCount > 0,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (billCount > 0)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = stringResource(R.string.content_description_decrement),
                        tint = if (billCount > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newText: String ->
                        if (newText.all { it.isDigit() } && newText.length <= 6) {
                            textFieldValue = newText
                            onValueChange(newText)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .width(88.dp)
                        .padding(horizontal = 2.dp),
                    placeholder = { Text("0") },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.3f
                        ),
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                IconButton(
                    onClick = {
                        val newVal = (billCount + 1).toString()
                        textFieldValue = newVal
                        onValueChange(newVal)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.content_description_increment),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}