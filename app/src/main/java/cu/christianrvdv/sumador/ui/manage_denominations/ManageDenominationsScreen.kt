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
    val currency by viewModel.currency.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }
    var showEditInfoDialog by remember { mutableStateOf(false) }

    var editingEntity by remember { mutableStateOf<CustomDenominationEntity?>(null) }
    var deletingEntity by remember { mutableStateOf<CustomDenominationEntity?>(null) }

    var inputText by remember { mutableStateOf("") }
    var isCoin by remember { mutableStateOf(false) }

    // Función para abrir el diálogo de edición con información previa
    fun startEdit(entity: CustomDenominationEntity) {
        editingEntity = entity
        inputText = if (entity.isCoin) {
            entity.denomination.toString()
        } else {
            (entity.denomination / 100).toString()
        }
        isCoin = entity.isCoin
        showEditInfoDialog = true
    }

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
                    IconButton(onClick = { showResetConfirmationDialog = true }) {
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
                onClick = {
                    inputText = ""
                    isCoin = false
                    showAddDialog = true
                },
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
                    onEdit = { startEdit(entity) },
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

        // Diálogo Agregar
        if (showAddDialog) {
            DenominationDialog(
                title = stringResource(R.string.add_denomination),
                inputText = inputText,
                isCoin = isCoin,
                onInputChange = { inputText = it },
                onCoinChange = { isCoin = it },
                onConfirm = {
                    val value = inputText.toIntOrNull()
                    if (value != null && value > 0) {
                        viewModel.addDenomination(value, isCoin)
                        inputText = ""
                        isCoin = false
                        showAddDialog = false
                    }
                },
                onDismiss = {
                    inputText = ""
                    isCoin = false
                    showAddDialog = false
                }
            )
        }

        // Diálogo de confirmación de restablecimiento
        if (showResetConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmationDialog = false },
                title = { Text(stringResource(R.string.reset_denominations_confirmation_title)) },
                text = { Text(stringResource(R.string.reset_denominations_confirmation_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetToDefaults()
                            showResetConfirmationDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.reset_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmationDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Diálogo informativo antes de editar
        if (showEditInfoDialog && editingEntity != null) {
            AlertDialog(
                onDismissRequest = {
                    showEditInfoDialog = false
                    editingEntity = null
                },
                title = { Text(stringResource(R.string.edit_denomination_info_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.edit_denomination_info_message,
                            currency // El nombre de la moneda (PESO, USD, EURO)
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEditInfoDialog = false
                            showEditDialog = true
                        }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showEditInfoDialog = false
                            editingEntity = null
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Diálogo Editar (se abre después de aceptar el informativo)
        if (showEditDialog && editingEntity != null) {
            DenominationDialog(
                title = stringResource(R.string.edit_denomination),
                inputText = inputText,
                isCoin = isCoin,
                onInputChange = { inputText = it },
                onCoinChange = { isCoin = it },
                onConfirm = {
                    val value = inputText.toIntOrNull()
                    if (value != null && value > 0) {
                        viewModel.updateDenomination(editingEntity!!, value, isCoin)
                        inputText = ""
                        isCoin = false
                        showEditDialog = false
                        editingEntity = null
                    }
                },
                onDismiss = {
                    inputText = ""
                    isCoin = false
                    showEditDialog = false
                    editingEntity = null
                }
            )
        }

        // Diálogo Eliminar
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
                            formatDenomination(deletingEntity!!.denomination, deletingEntity!!.isCoin)
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
                text = formatDenomination(entity.denomination, entity.isCoin),
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
    isCoin: Boolean,
    onInputChange: (String) -> Unit,
    onCoinChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(R.string.denomination_value_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(text = "Tipo:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = !isCoin,
                        onClick = { onCoinChange(false) }
                    )
                    Text("Billete")
                    RadioButton(
                        selected = isCoin,
                        onClick = { onCoinChange(true) }
                    )
                    Text("Moneda")
                }
            }
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