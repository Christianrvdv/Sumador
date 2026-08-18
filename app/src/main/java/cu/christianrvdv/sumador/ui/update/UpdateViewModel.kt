package cu.christianrvdv.sumador.ui.update

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.christianrvdv.sumador.SumadorApplication
import cu.christianrvdv.sumador.utils.DownloadWorker
import cu.christianrvdv.sumador.utils.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val updateManager = UpdateManager(context)

    private val _updateInfo = MutableStateFlow<UpdateManager.UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateManager.UpdateInfo?> = _updateInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _showCheckingDialog = MutableStateFlow(false)
    val showCheckingDialog: StateFlow<Boolean> = _showCheckingDialog.asStateFlow()

    private val _showNoUpdateDialog = MutableStateFlow(false)
    val showNoUpdateDialog: StateFlow<Boolean> = _showNoUpdateDialog.asStateFlow()

    private val _showUpdateErrorDialog = MutableStateFlow(false)
    val showUpdateErrorDialog: StateFlow<Boolean> = _showUpdateErrorDialog.asStateFlow()

    private val _showNetworkErrorDialog = MutableStateFlow(false)
    val showNetworkErrorDialog: StateFlow<Boolean> = _showNetworkErrorDialog.asStateFlow()

    private val _showDownloadStartedDialog = MutableStateFlow(false)
    val showDownloadStartedDialog: StateFlow<Boolean> = _showDownloadStartedDialog.asStateFlow()

    private var autoCheckDone = false

    fun checkForUpdatesOnStart() {
        if (autoCheckDone) return
        autoCheckDone = true
        viewModelScope.launch {
            val result = updateManager.checkForUpdate()
            when (result) {
                is UpdateManager.UpdateResult.Success -> {
                    _updateInfo.value = result.info
                    _showUpdateDialog.value = true
                }
                else -> {
                    // Silencioso
                }
            }
        }
    }

    suspend fun checkForUpdatesManually() {
        _showCheckingDialog.value = true
        try {
            val result = updateManager.checkForUpdate()
            _showCheckingDialog.value = false
            when (result) {
                is UpdateManager.UpdateResult.Success -> {
                    _updateInfo.value = result.info
                    _showUpdateDialog.value = true
                }
                UpdateManager.UpdateResult.NoUpdate -> {
                    _showNoUpdateDialog.value = true
                }
                UpdateManager.UpdateResult.NetworkError -> {
                    _showNetworkErrorDialog.value = true
                }
                is UpdateManager.UpdateResult.Error -> {
                    _showUpdateErrorDialog.value = true
                }
            }
        } catch (e: Exception) {
            _showCheckingDialog.value = false
            _showUpdateErrorDialog.value = true
        }
    }

    fun onUpdateConfirmed() {
        val info = _updateInfo.value ?: return
        _showUpdateDialog.value = false
        (context.applicationContext as SumadorApplication).pendingUpdate = info
        updateManager.startBackgroundDownload(info.downloadUrl, info.version)
        _showDownloadStartedDialog.value = true
    }

    fun dismissDialogs() {
        _showUpdateDialog.value = false
        _showCheckingDialog.value = false
        _showNoUpdateDialog.value = false
        _showUpdateErrorDialog.value = false
        _showNetworkErrorDialog.value = false
        _showDownloadStartedDialog.value = false
    }

    fun handlePendingUpdate() {
        val pending = (context.applicationContext as SumadorApplication).pendingUpdate
        if (pending != null) {
            val permisoConcedido = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
            if (permisoConcedido) {
                val apkFile = DownloadWorker.Companion.getApkFile(context, pending.version)
                if (apkFile.exists() && DownloadWorker.Companion.isApkValid(context, apkFile)) {
                    updateManager.startBackgroundDownload(pending.downloadUrl, pending.version)
                    (context.applicationContext as SumadorApplication).pendingUpdate = null
                } else {
                    if (apkFile.exists()) apkFile.delete()
                    updateManager.startBackgroundDownload(pending.downloadUrl, pending.version)
                }
            }
        }
    }
}