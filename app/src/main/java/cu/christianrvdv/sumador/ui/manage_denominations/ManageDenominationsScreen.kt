package cu.christianrvdv.sumador.ui.manage_denominations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cu.christianrvdv.sumador.R
import cu.christianrvdv.sumador.data.database.CustomDenominationEntity
import cu.christianrvdv.sumador.ui.sumador.formatDenomination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDenominationsScreen(
    onBack: () -> Unit,
    viewModel: CustomDenominationViewModel = hiltViewModel()
) {
    val denominations by viewModel.denominations.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingEntity by remember { mutableStateOf<CustomDenominationEntity?>(null) }
    var deletingEntity by remember { mutableStateOf<CustomDenominationEntity?>(null) }

    // Estado para el campo de texto en diálogos
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_denominations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back_label))
                    }
                },
                actions = {
                    // Botón para restablecer a las predeterminadas
                    IconButton(onClick = {
                        viewModel.resetToDefaults()
                    }) {
                        Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.reset_to_defaults))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_denomination))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(denominations) { entity ->
                DenominationItem(
                    entity = entity,
                    onEdit = {
                        editingEntity = entity
                        inputText = entity.denomination.toString()
                        showEditDialog = true
                    },
                    onDelete = {
                        deletingEntity = entity
                        showDeleteDialog = true
                    }
                )
            }
            if (denominations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                Icons.Default.Money,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_denominations_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.add_denomination_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Diálogo para agregar
        if (showAddDialog) {
            DenominationDialog(
                title = stringResource(R.string.add_denomination),
                inputText = inputText,
                onInputChange = { inputText = it },
                onConfirm = {
                    val value = inputText.toIntOrNull()
                    if (value != null && value > 0) {
                        viewModel.addDenomination(value)
                        inputText = ""
                        showAddDialog = false
                    }
                },
                onDismiss = {
                    inputText = ""
                    showAddDialog = false
                }
            )
        }

        // Diálogo para editar
        if (showEditDialog && editingEntity != null) {
            DenominationDialog(
                title = stringResource(R.string.edit_denomination),
                inputText = inputText,
                onInputChange = { inputText = it },
                onConfirm = {
                    val value = inputText.toIntOrNull()
                    if (value != null && value > 0) {
                        viewModel.updateDenomination(editingEntity!!, value)
                        inputText = ""
                        showEditDialog = false
                        editingEntity = null
                    }
                },
                onDismiss = {
                    inputText = ""
                    showEditDialog = false
                    editingEntity = null
                }
            )
        }

        // Diálogo de confirmación para eliminar
        if (showDeleteDialog && deletingEntity != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    deletingEntity = null
                },
                title = { Text(stringResource(R.string.delete_denomination_confirmation)) },
                text = {
                    Text(
                        stringResource(
                            R.string.delete_denomination_message,
                            formatDenomination(deletingEntity!!.denomination)
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteDenomination(deletingEntity!!)
                            showDeleteDialog = false
                            deletingEntity = null
                        }
                    ) {
                        Text(stringResource(R.string.delete_label))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        deletingEntity = null
                    }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun DenominationItem(
    entity: CustomDenominationEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDenomination(entity.denomination),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_button))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_label))
                }
            }
        }
    }
}

@Composable
private fun DenominationDialog(
    title: String,
    inputText: String,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                label = { Text(stringResource(R.string.denomination_value_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}