package cu.christianrvdv.sumador.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cu.christianrvdv.sumador.R

@Composable
fun SettingsDialog(
    settingsState: SettingsState,
    onDismiss: () -> Unit,
    onThemeChange: (ThemeOption) -> Unit,
    onCurrencyChange: (CurrencySymbol) -> Unit,
    onSortChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Tema
                Text(
                    text = stringResource(R.string.theme_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                ThemeOption.values().forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settingsState.theme == option,
                            onClick = { onThemeChange(option) }
                        )
                        Text(
                            text = when (option) {
                                ThemeOption.LIGHT -> stringResource(R.string.theme_light)
                                ThemeOption.DARK -> stringResource(R.string.theme_dark)
                                ThemeOption.SYSTEM -> stringResource(R.string.theme_system)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Símbolo de moneda
                Text(
                    text = stringResource(R.string.currency_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                CurrencySymbol.values().forEach { currency ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settingsState.currencySymbol == currency,
                            onClick = { onCurrencyChange(currency) }
                        )
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Orden de billetes
                Text(
                    text = stringResource(R.string.sort_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settingsState.sortAscending,
                        onClick = { onSortChange(true) }
                    )
                    Text(stringResource(R.string.sort_ascending), style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !settingsState.sortAscending,
                        onClick = { onSortChange(false) }
                    )
                    Text(stringResource(R.string.sort_descending), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}