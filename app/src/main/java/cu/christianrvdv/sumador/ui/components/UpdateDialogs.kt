package cu.christianrvdv.sumador.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cu.christianrvdv.sumador.R
import cu.christianrvdv.sumador.ui.update.UpdateViewModel

@Composable
fun UpdateDialogs(viewModel: UpdateViewModel) {
    val updateInfo by viewModel.updateInfo.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val showCheckingDialog by viewModel.showCheckingDialog.collectAsState()
    val showNoUpdateDialog by viewModel.showNoUpdateDialog.collectAsState()
    val showUpdateErrorDialog by viewModel.showUpdateErrorDialog.collectAsState()
    val showNetworkErrorDialog by viewModel.showNetworkErrorDialog.collectAsState()
    val showDownloadStartedDialog by viewModel.showDownloadStartedDialog.collectAsState()

    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text(stringResource(R.string.update_dialog_available_title)) },
            text = {
                val info = updateInfo!!
                Text(
                    if (info.alreadyDownloaded) {
                        stringResource(R.string.update_dialog_already_downloaded_message, info.version)
                    } else {
                        stringResource(R.string.update_dialog_available_message, info.version)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onUpdateConfirmed() }) {
                    Text(stringResource(R.string.update_dialog_update_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text(stringResource(R.string.update_dialog_cancel_button))
                }
            }
        )
    }

    if (showDownloadStartedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text(stringResource(R.string.update_dialog_download_started_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.update_dialog_download_started_message))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_download_started_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text(stringResource(R.string.update_dialog_accept_button))
                }
            }
        )
    }

    if (showCheckingDialog) {
        Dialog(onDismissRequest = { }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.update_dialog_checking_title))
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showNoUpdateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text(stringResource(R.string.update_dialog_no_update_title)) },
            text = { Text(stringResource(R.string.update_dialog_no_update_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text(stringResource(R.string.update_dialog_accept_button))
                }
            }
        )
    }

    if (showUpdateErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text(stringResource(R.string.update_dialog_error_title)) },
            text = { Text(stringResource(R.string.update_dialog_error_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text(stringResource(R.string.update_dialog_accept_button))
                }
            }
        )
    }

    if (showNetworkErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text(stringResource(R.string.update_dialog_network_error_title)) },
            text = { Text(stringResource(R.string.update_dialog_network_error_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text(stringResource(R.string.update_dialog_accept_button))
                }
            }
        )
    }
}