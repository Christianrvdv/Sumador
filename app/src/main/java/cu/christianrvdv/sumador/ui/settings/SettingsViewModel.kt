// ui/settings/SettingsViewModel.kt
package cu.christianrvdv.sumador.ui.settings

import android.app.backup.BackupManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.christianrvdv.sumador.data.BackupData
import cu.christianrvdv.sumador.data.database.CustomDenominationDao
import cu.christianrvdv.sumador.data.database.SavedSumDao
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "settings")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val context: Context,
    private val savedSumDao: SavedSumDao,
    private val customDenominationDao: CustomDenominationDao,
    private val backupManager: BackupManager
) : ViewModel() {

    // Clave fija de 16 bytes para AES-128 (cambiar en producción)
    private val ENCRYPTION_KEY = "SumadorBackupKey"  // 16 caracteres

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val CURRENCY = stringPreferencesKey("currency")
        val SORT_ASC = booleanPreferencesKey("sort_asc")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val CONFIRM_CLEAR = booleanPreferencesKey("confirm_clear")
        val LANGUAGE = stringPreferencesKey("language")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val USE_COINS = booleanPreferencesKey("use_coins")
    }

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data
                .catch { exception ->
                    Log.e("SettingsViewModel", "Error reading settings", exception)
                    emit(emptyPreferences())
                }
                .collect { prefs ->
                    try {
                        val themeStr = prefs[Keys.THEME] ?: "SYSTEM"
                        val theme = ThemeOption.valueOf(themeStr)
                        val currencyStr = prefs[Keys.CURRENCY] ?: "PESO"
                        val currency = CurrencySymbol.valueOf(currencyStr)
                        val sortAsc = prefs[Keys.SORT_ASC] ?: true
                        val autoSave = prefs[Keys.AUTO_SAVE] ?: true
                        val confirmClear = prefs[Keys.CONFIRM_CLEAR] ?: true
                        val languageStr = prefs[Keys.LANGUAGE] ?: "SYSTEM"
                        val language = LanguageOption.valueOf(languageStr)
                        val keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false
                        val useCoins = prefs[Keys.USE_COINS] ?: false

                        _state.value = SettingsState(
                            theme = theme,
                            currencySymbol = currency,
                            sortAscending = sortAsc,
                            autoSave = autoSave,
                            confirmClear = confirmClear,
                            language = language,
                            keepScreenOn = keepScreenOn,
                            useCoins = useCoins,
                            lastBackupTime = null
                        )
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error parsing settings", e)
                    }
                }
        }
    }

    // ---- Métodos de actualización de ajustes ----
    suspend fun updateTheme(theme: ThemeOption) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.THEME] = theme.name
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving theme", e)
        }
    }

    suspend fun updateCurrency(currency: CurrencySymbol) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.CURRENCY] = currency.name
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving currency", e)
        }
    }

    suspend fun updateSortOrder(ascending: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.SORT_ASC] = ascending
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving sort order", e)
        }
    }

    suspend fun updateAutoSave(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.AUTO_SAVE] = enabled
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving autoSave", e)
        }
    }

    suspend fun updateConfirmClear(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.CONFIRM_CLEAR] = enabled
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving confirmClear", e)
        }
    }

    suspend fun updateLanguage(language: LanguageOption) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.LANGUAGE] = language.name
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving language", e)
        }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.KEEP_SCREEN_ON] = enabled
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving keepScreenOn", e)
        }
    }

    suspend fun updateUseCoins(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.USE_COINS] = enabled
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving useCoins", e)
        }
    }

    // ---- Método para backup automático (Google Auto Backup) ----
    suspend fun requestBackup(): Result<Unit> {
        return try {
            backupManager.dataChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error notifying backup", e)
            Result.failure(e)
        }
    }

    // ---- Funciones de cifrado/descifrado ----
    private fun encryptData(data: String): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(Charsets.UTF_8), "AES")
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return iv + encrypted
    }

    private fun decryptData(encryptedBytes: ByteArray): String {
        val iv = encryptedBytes.copyOfRange(0, 16)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(Charsets.UTF_8), "AES")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        val decrypted = cipher.doFinal(encryptedBytes.copyOfRange(16, encryptedBytes.size))
        return String(decrypted, Charsets.UTF_8)
    }

    // ---- Exportación manual (cifrada) ----
    suspend fun exportDataToUri(context: Context, uri: Uri): Result<Unit> {
        return try {
            val sums = savedSumDao.getAll()
            val denominations = customDenominationDao.getAll()
            val settings = _state.value
            val backupData = BackupData(
                version = 1,
                exportDate = System.currentTimeMillis(),
                settings = settings,
                savedSums = sums,
                customDenominations = denominations
            )
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(backupData)
            val encryptedBytes = encryptData(json)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(encryptedBytes)
            } ?: return Result.failure(Exception("Could not open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error exporting to URI", e)
            Result.failure(e)
        }
    }

    // ---- Importación manual (descifrada) ----
    suspend fun importDataFromUri(context: Context, uri: Uri): Result<Unit> {
        return try {
            val encryptedBytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: return Result.failure(Exception("Could not open input stream"))

            val json = decryptData(encryptedBytes)
            val gson = Gson()
            val backupData = gson.fromJson(json, BackupData::class.java)

            // Validar versión
            if (backupData.version != 1) {
                return Result.failure(IllegalArgumentException("Unsupported backup version"))
            }

            // Limpiar y restaurar
            savedSumDao.deleteAll()
            customDenominationDao.deleteAll()
            savedSumDao.insertAll(backupData.savedSums)
            backupData.customDenominations.forEach { customDenominationDao.insert(it) }

            // Restaurar ajustes
            context.dataStore.edit { prefs ->
                prefs[Keys.THEME] = backupData.settings.theme.name
                prefs[Keys.CURRENCY] = backupData.settings.currencySymbol.name
                prefs[Keys.SORT_ASC] = backupData.settings.sortAscending
                prefs[Keys.AUTO_SAVE] = backupData.settings.autoSave
                prefs[Keys.CONFIRM_CLEAR] = backupData.settings.confirmClear
                prefs[Keys.LANGUAGE] = backupData.settings.language.name
                prefs[Keys.KEEP_SCREEN_ON] = backupData.settings.keepScreenOn
                prefs[Keys.USE_COINS] = backupData.settings.useCoins
            }

            _state.value = backupData.settings

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error importing from URI", e)
            Result.failure(e)
        }
    }
}