package cu.christianrvdv.sumador.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cu.christianrvdv.sumador.R
import cu.christianrvdv.sumador.data.database.Converters
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import cu.christianrvdv.sumador.ui.settings.CurrencySymbol
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSumsScreen(
    onBack: () -> Unit,
    viewModel: SavedSumsViewModel = hiltViewModel()
) {
    val savedSums by viewModel.allSavedSums.collectAsState(initial = emptyList())
    var selectedSum by remember { mutableStateOf<SavedSumEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (savedSums.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedSums) { sum ->
                    SavedSumItem(
                        sum = sum,
                        onItemClick = { selectedSum = sum },
                        onDelete = { viewModel.delete(sum) }
                    )
                }
            }
        }
    }

    // Diálogo de detalle con edición
    selectedSum?.let { sum ->
        SavedSumDetailDialog(
            savedSum = sum,
            onDismiss = { selectedSum = null },
            onUpdateName = { newName ->
                viewModel.update(sum.copy(name = newName))
            }
        )
    }
}

@Composable
fun SavedSumItem(
    sum: SavedSumEntity,
    onItemClick: () -> Unit,
    onDelete: () -> Unit
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
                    text = "${sum.total} ${CurrencySymbol.PESO.symbol}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSumDetailDialog(
    savedSum: SavedSumEntity,
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val denominations = Converters().fromStringToMap(savedSum.denominationsMap)

    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(savedSum) { mutableStateOf(savedSum.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (isEditing) {
                TextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre") },
                    singleLine = true
                )
            } else {
                Text(text = savedSum.name)
            }
        },
        text = {
            Column {
                Text(text = "Fecha: ${dateFormat.format(savedSum.timestamp)}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Total: ${savedSum.total} ${CurrencySymbol.PESO.symbol}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Detalle:", fontWeight = FontWeight.Bold)
                denominations.entries.sortedBy { it.key }.forEach { (denom, count) ->
                    if (count > 0) {
                        Text(text = "$denom x $count = ${denom * count}")
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            onUpdateName(editedName)
                            isEditing = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            } else {
                TextButton(onClick = { onDismiss() }) {
                    Text("Cerrar")
                }
            }
        },
        dismissButton = {
            if (isEditing) {
                TextButton(onClick = { isEditing = false }) {
                    Text("Cancelar")
                }
            } else {
                TextButton(onClick = { isEditing = true }) {
                    Text("Editar")
                }
            }
        }
    )
}