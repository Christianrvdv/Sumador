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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var localTotalMinStr by remember { mutableStateOf<String?>(null) }
    var localTotalMaxStr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showFilterDialog) {
        if (showFilterDialog) {
            localDateFrom = filterState.dateFrom
            localDateTo = filterState.dateTo
            localTotalMinStr = filterState.totalMin?.let { cents ->
                val value = cents.toDouble() / 100.0
                if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
            }
            localTotalMaxStr = filterState.totalMax?.let { cents ->
                val value = cents.toDouble() / 100.0
                if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
            }
        }
    }

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

    val hasActiveFilters = filterState.dateFrom != null ||
            filterState.dateTo != null ||
            filterState.totalMin != null ||
            filterState.totalMax != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.history_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back_label))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.clearFilters()
                        searchText = ""
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_button))
                    }
                    BadgedBox(
                        badge = {
                            if (hasActiveFilters) {
                                Badge { Text("•") }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.advanced_filters_title))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
            ) { }

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
                val totalMinCents = localTotalMinStr?.let { str ->
                    try {
                        (str.replace(',', '.').toDouble() * 100).toLong()
                    } catch (_: NumberFormatException) {
                        null
                    }
                }
                val totalMaxCents = localTotalMaxStr?.let { str ->
                    try {
                        (str.replace(',', '.').toDouble() * 100).toLong()
                    } catch (_: NumberFormatException) {
                        null
                    }
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.FilterAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.advanced_filters_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_button))
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.date_range_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                DateFilterRow(
                    label = stringResource(R.string.date_from),
                    date = dateFrom,
                    onDateChange = onDateFromChange,
                    endOfDay = false
                )

                DateFilterRow(
                    label = stringResource(R.string.date_to),
                    date = dateTo,
                    onDateChange = onDateToChange,
                    endOfDay = true
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.amount_range_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                AmountFilterField(
                    label = stringResource(R.string.amount_min),
                    value = totalMinStr,
                    onValueChange = onTotalMinStrChange
                )

                AmountFilterField(
                    label = stringResource(R.string.amount_max),
                    value = totalMaxStr,
                    onValueChange = onTotalMaxStrChange
                )

                Text(
                    text = stringResource(R.string.amount_instruction),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.clear_button))
                    }

                    Button(
                        onClick = onApply,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.apply_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateFilterRow(
    label: String,
    date: Long?,
    onDateChange: (Long?) -> Unit,
    endOfDay: Boolean = false
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val openPicker = {
        val calendar = Calendar.getInstance()
        date?.let { calendar.timeInMillis = it }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                    if (endOfDay) {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    } else {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }.timeInMillis
                onDateChange(selected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .clickable { openPicker() }
        ) {
            OutlinedTextField(
                value = date?.let { dateFormat.format(Date(it)) } ?: "",
                onValueChange = {},
                enabled = false,
                readOnly = true,
                label = { Text(label) },
                placeholder = { Text(stringResource(R.string.date_placeholder)) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        if (date != null) {
            IconButton(
                onClick = { onDateChange(null) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_button))
            }
        } else {
            Spacer(Modifier.width(40.dp))
        }
    }
}

@Composable
private fun AmountFilterField(
    label: String,
    value: String?,
    onValueChange: (String?) -> Unit
) {
    OutlinedTextField(
        value = value ?: "",
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
            val valid = filtered.replace(',', '.').let { str ->
                val parts = str.split('.')
                parts.size <= 2 && parts.all { part -> part.all { it.isDigit() } }
            }
            if (valid) {
                onValueChange(filtered.ifEmpty { null })
            }
        },
        label = { Text(label) },
        placeholder = { Text("0.00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = {
            if (value != null) {
                IconButton(onClick = { onValueChange(null) }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_button))
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

fun shareSum(context: android.content.Context, sum: SavedSumEntity) {
    val converter = Converters()
    val denominations = converter.fromStringToMap(sum.denominationsMap)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val sb = StringBuilder()
    sb.append("📊 ${sum.name}\n")
    sb.append("📅 ${dateFormat.format(sum.timestamp)}\n")
    sb.append("💰 ${context.getString(R.string.total_label, formatCurrency(sum.total))}\n\n")
    sb.append("${context.getString(R.string.detail_label)}:\n")
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
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_sum)))
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
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_label))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_label))
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
                        text = stringResource(R.string.total_label, formatCurrency(savedSum.total) + " " + CurrencySymbol.PESO.symbol),
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
                        contentDescription = stringResource(R.string.share_label),
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