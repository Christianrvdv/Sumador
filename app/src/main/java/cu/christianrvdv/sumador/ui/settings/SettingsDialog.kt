package cu.christianrvdv.sumador.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cu.christianrvdv.sumador.R

@Composable
fun SettingsDialog(
    settingsState: SettingsState,
    onDismiss: () -> Unit,
    onThemeChange: (ThemeOption) -> Unit,
    onCurrencyChange: (CurrencySymbol) -> Unit,
    onSortChange: (Boolean) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onConfirmClearChange: (Boolean) -> Unit,
    onLanguageChange: (LanguageOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // --- Tema ---
                SettingsSectionHeader(
                    icon = Icons.Default.BrightnessMedium,
                    title = stringResource(R.string.theme_label)
                )
                Spacer(Modifier.height(4.dp))
                ThemeOption.values().forEach { option ->
                    SettingsRadioRow(
                        selected = settingsState.theme == option,
                        label = when (option) {
                            ThemeOption.LIGHT -> stringResource(R.string.theme_light)
                            ThemeOption.DARK  -> stringResource(R.string.theme_dark)
                            ThemeOption.SYSTEM -> stringResource(R.string.theme_system)
                        },
                        onClick = { onThemeChange(option) }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // --- Moneda ---
                SettingsSectionHeader(
                    icon = Icons.Default.AttachMoney,
                    title = stringResource(R.string.currency_label)
                )
                Spacer(Modifier.height(4.dp))
                CurrencySymbol.values().forEach { currency ->
                    SettingsRadioRow(
                        selected = settingsState.currencySymbol == currency,
                        label = when (currency) {
                            CurrencySymbol.PESO -> stringResource(R.string.currency_peso)
                            CurrencySymbol.USD  -> stringResource(R.string.currency_usd)
                            CurrencySymbol.EURO -> stringResource(R.string.currency_euro)
                        },
                        onClick = { onCurrencyChange(currency) }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // --- Idioma ---
                SettingsSectionHeader(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language_label)
                )
                Spacer(Modifier.height(4.dp))
                LanguageOption.values().forEach { option ->
                    SettingsRadioRow(
                        selected = settingsState.language == option,
                        label = when (option) {
                            LanguageOption.ENGLISH -> stringResource(R.string.language_english)
                            LanguageOption.SPANISH -> stringResource(R.string.language_spanish)
                            LanguageOption.SYSTEM  -> stringResource(R.string.language_system)
                        },
                        onClick = { onLanguageChange(option) }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // --- Orden de billetes ---
                SettingsSectionHeader(
                    icon = Icons.Default.Sort,
                    title = stringResource(R.string.sort_label)
                )
                Spacer(Modifier.height(4.dp))
                SettingsRadioRow(
                    selected = settingsState.sortAscending,
                    label = stringResource(R.string.sort_ascending),
                    onClick = { onSortChange(true) }
                )
                SettingsRadioRow(
                    selected = !settingsState.sortAscending,
                    label = stringResource(R.string.sort_descending),
                    onClick = { onSortChange(false) }
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // --- Guardado automático ---
                SettingsSwitchRow(
                    label = stringResource(R.string.auto_save_label),
                    checked = settingsState.autoSave,
                    onCheckedChange = onAutoSaveChange
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // --- Confirmar limpiar ---
                SettingsSwitchRow(
                    label = stringResource(R.string.confirm_clear_label),
                    checked = settingsState.confirmClear,
                    onCheckedChange = onConfirmClearChange
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.ok),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

// Componentes auxiliares con estilos actualizados
@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsRadioRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(48.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}