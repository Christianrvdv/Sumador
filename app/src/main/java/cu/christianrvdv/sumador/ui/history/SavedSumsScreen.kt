// ui/history/SavedSumsScreen.kt
package cu.christianrvdv.sumador.ui.history

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cu.christianrvdv.sumador.R
import cu.christianrvdv.sumador.data.database.Converters
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import cu.christianrvdv.sumador.ui.settings.CurrencySymbol
import cu.christianrvdv.sumador.ui.sumador.formatCurrency
import cu.christianrvdv.sumador.ui.sumador.formatDenomination
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSumsScreen(
    onBack: () -> Unit,
    viewModel: SavedSumsViewModel = hiltViewModel()
) {
    val savedSums by viewModel.allSavedSums.collectAsState(initial = emptyList())
    val filterState by viewModel.filterState.collectAsState()
    var selectedSum by remember { mutableStateOf<SavedSumEntity?>(null) }

    var searchText by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Estados locales para el diálogo de filtros (en unidad principal)
    var localDateFrom by remember { mutableStateOf<Long?>(null) }
    var localDateTo by remember { mutableStateOf<Long?>(null) }
    var localTotalMinStr by remember { mutableStateOf<String?>(null) } // en unidad principal, ej "60000.50"
    var localTotalMaxStr by remember { mutableStateOf<String?>(null) }

    // Cargar valores actuales al abrir el diálogo (convertir de céntimos a unidad principal)
    LaunchedEffect(showFilterDialog) {
        if (showFilterDialog) {
            localDateFrom = filterState.dateFrom
            localDateTo = filterState.dateTo
            localTotalMinStr = filterState.totalMin?.let { cents ->
                // Convertir céntimos a unidad principal con dos decimales
                val value = cents.toDouble() / 100.0
                if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
            }
            localTotalMaxStr = filterState.totalMax?.let { cents ->
                val value = cents.toDouble() / 100.0
                if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
            }
        }
    }

    // Aplicar filtro de nombre, manteniendo los otros filtros (que ya están en filterState)
    LaunchedEffect(searchText) {
        val name = if (searchText.isBlank()) null else searchText
        viewModel.setFilter(
            name = name,
            dateFrom = filterState.dateFrom,
            dateTo = filterState.dateTo,
            totalMin = filterState.totalMin,
            totalMax = filterState.totalMax
        )
    }

    // Determinar si hay filtros activos (distintos del nombre)
    val hasActiveFilters = filterState.dateFrom != null ||
            filterState.dateTo != null ||
            filterState.totalMin != null ||
            filterState.totalMax != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.history_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.clearFilters()
                        searchText = ""
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear filters")
                    }
                    BadgedBox(
                        badge = {
                            if (hasActiveFilters) {
                                Badge { Text("•") }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchBar(
                query = searchText,
                onQueryChange = { searchText = it },
                onSearch = { },
                active = false,
                onActiveChange = { },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // No se usa para resultados en este caso
            }

            if (savedSums.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedSums) { sum ->
                        SavedSumItem(
                            sum = sum,
                            onItemClick = { selectedSum = sum },
                            onDelete = { viewModel.delete(sum) },
                            onShare = { shareSum(context, sum) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de filtros avanzados
    if (showFilterDialog) {
        FilterDialog(
            dateFrom = localDateFrom,
            dateTo = localDateTo,
            totalMinStr = localTotalMinStr,
            totalMaxStr = localTotalMaxStr,
            onDateFromChange = { localDateFrom = it },
            onDateToChange = { localDateTo = it },
            onTotalMinStrChange = { localTotalMinStr = it },
            onTotalMaxStrChange = { localTotalMaxStr = it },
            onApply = {
                // Convertir de unidad principal a céntimos
                val totalMinCents = localTotalMinStr?.let { str ->
                    try {
                        (str.replace(',', '.').toDouble() * 100).toLong()
                    } catch (_: NumberFormatException) { null }
                }
                val totalMaxCents = localTotalMaxStr?.let { str ->
                    try {
                        (str.replace(',', '.').toDouble() * 100).toLong()
                    } catch (_: NumberFormatException) { null }
                }
                viewModel.setFilter(
                    name = if (searchText.isBlank()) null else searchText,
                    dateFrom = localDateFrom,
                    dateTo = localDateTo,
                    totalMin = totalMinCents,
                    totalMax = totalMaxCents
                )
                showFilterDialog = false
            },
            onClear = {
                viewModel.clearFilters()
                searchText = ""
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    // BottomSheet para detalle de una suma guardada
    selectedSum?.let { sum ->
        SavedSumDetailBottomSheet(
            savedSum = sum,
            onDismiss = { selectedSum = null },
            onUpdateName = { newName ->
                viewModel.update(sum.copy(name = newName))
            },
            onShare = { shareSum(context, sum) }
        )
    }
}

// ===== DIÁLOGO DE FILTROS =====
@Composable
private fun FilterDialog(
    dateFrom: Long?,
    dateTo: Long?,
    totalMinStr: String?,
    totalMaxStr: String?,
    onDateFromChange: (Long?) -> Unit,
    onDateToChange: (Long?) -> Unit,
    onTotalMinStrChange: (String?) -> Unit,
    onTotalMaxStrChange: (String?) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtros avanzados") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fecha desde
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Desde:", modifier = Modifier.weight(0.3f))
                    OutlinedTextField(
                        value = dateFrom?.let { dateFormat.format(Date(it)) } ?: "",
                        onValueChange = { /* Solo lectura, se usa el DatePicker */ },
                        readOnly = true,
                        modifier = Modifier.weight(0.5f),
                        placeholder = { Text("dd/mm/aaaa") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                val calendar = Calendar.getInstance()
                                dateFrom?.let { calendar.timeInMillis = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selected = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        onDateFromChange(selected)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                            }
                        }
                    )
                    IconButton(onClick = { onDateFromChange(null) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }

                // Fecha hasta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Hasta:", modifier = Modifier.weight(0.3f))
                    OutlinedTextField(
                        value = dateTo?.let { dateFormat.format(Date(it)) } ?: "",
                        onValueChange = { /* Solo lectura */ },
                        readOnly = true,
                        modifier = Modifier.weight(0.5f),
                        placeholder = { Text("dd/mm/aaaa") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                val calendar = Calendar.getInstance()
                                dateTo?.let { calendar.timeInMillis = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selected = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth)
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                            set(Calendar.SECOND, 59)
                                            set(Calendar.MILLISECOND, 999)
                                        }.timeInMillis
                                        onDateToChange(selected)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                            }
                        }
                    )
                    IconButton(onClick = { onDateToChange(null) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }

                Divider()

                // Total mínimo (permite decimales)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Mínimo:", modifier = Modifier.weight(0.3f))
                    OutlinedTextField(
                        value = totalMinStr ?: "",
                        onValueChange = { newValue ->
                            // Permitir solo dígitos, punto o coma (como separador decimal)
                            val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
                            // Evitar múltiples puntos/comas
                            val valid = filtered.replace(',', '.')
                                .let { str ->
                                    val parts = str.split('.')
                                    parts.size <= 2 && parts.all { it.all { c -> c.isDigit() } }
                                }
                            if (valid) {
                                onTotalMinStrChange(filtered.ifEmpty { null })
                            }
                        },
                        modifier = Modifier.weight(0.5f),
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    IconButton(onClick = { onTotalMinStrChange(null) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }

                // Total máximo (permite decimales)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Máximo:", modifier = Modifier.weight(0.3f))
                    OutlinedTextField(
                        value = totalMaxStr ?: "",
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
                            val valid = filtered.replace(',', '.')
                                .let { str ->
                                    val parts = str.split('.')
                                    parts.size <= 2 && parts.all { it.all { c -> c.isDigit() } }
                                }
                            if (valid) {
                                onTotalMaxStrChange(filtered.ifEmpty { null })
                            }
                        },
                        modifier = Modifier.weight(0.5f),
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    IconButton(onClick = { onTotalMaxStrChange(null) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }

                // Indicador de unidad
                Text(
                    text = "Los montos deben ingresarse en la moneda principal (ej. 60000.50).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onApply) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) {
                    Text("Limpiar todo")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

// ===== FUNCIONES DE COMPARTIR Y COMPONENTES AUXILIARES =====

fun shareSum(context: android.content.Context, sum: SavedSumEntity) {
    val converter = Converters()
    val denominations = converter.fromStringToMap(sum.denominationsMap)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val sb = StringBuilder()
    sb.append("📊 ${sum.name}\n")
    sb.append("📅 ${dateFormat.format(sum.timestamp)}\n")
    sb.append("💰 Total: ${formatCurrency(sum.total)}\n\n")
    sb.append("Detalle:\n")
    denominations.entries.sortedByDescending { it.key }.forEach { (denom, count) ->
        if (count > 0) {
            sb.append("  ${formatDenomination(denom)} x $count = ${formatCurrency((denom * count).toLong())}\n")
        }
    }
    val shareText = sb.toString()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir suma"))
}

@Composable
fun SavedSumItem(
    sum: SavedSumEntity,
    onItemClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = sum.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(sum.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatCurrency(sum.total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSumDetailBottomSheet(
    savedSum: SavedSumEntity,
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val denominations = Converters().fromStringToMap(savedSum.denominationsMap)

    var showEditDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = savedSum.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.date_label, dateFormat.format(savedSum.timestamp)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.total_label, "${formatCurrency(savedSum.total)} ${CurrencySymbol.PESO.symbol}"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detail_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    denominations.entries.sortedBy { it.key }.forEach { (denom, count) ->
                        if (count > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${formatDenomination(denom)} x $count",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "= ${formatCurrency((denom * count).toLong())}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.close_button))
                }
                TextButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.edit_button))
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        var editedName by remember { mutableStateOf(savedSum.name) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_name_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.edit_name_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text(stringResource(R.string.name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            onUpdateName(editedName)
                            showEditDialog = false
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(R.string.save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}