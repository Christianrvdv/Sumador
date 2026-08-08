// SettingsDialog.kt
package cu.christianrvdv.sumador.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
        title = { Text("Configuraciones") },
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
                    text = "Tema",
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
                                ThemeOption.LIGHT -> "Claro"
                                ThemeOption.DARK -> "Oscuro"
                                ThemeOption.SYSTEM -> "Automático (sistema)"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Símbolo de moneda
                Text(
                    text = "Símbolo de moneda",
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
                    text = "Orden de billetes",
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
                    Text("Ascendente (5,10,20...)", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !settingsState.sortAscending,
                        onClick = { onSortChange(false) }
                    )
                    Text("Descendente (5000,2000,1000...)", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Aceptar")
            }
        }
    )
}