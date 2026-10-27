package app.trackar.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState

    /** User must allow "install unknown apps" for Trackar first. */
    data class NeedsPermission(val manifest: UpdateManifest) : UpdateState

    /** [progress] is 0..1, or negative when the size is unknown. */
    data class Downloading(val manifest: UpdateManifest, val progress: Float) : UpdateState

    data class Failed(val manifest: UpdateManifest) : UpdateState
}

/** Checks for a newer test build on app start and installs it automatically. */
@HiltViewModel
class UpdateViewModel @Inject constructor(private val updater: AppUpdater) : ViewModel() {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    init {
        viewModelScope.launch { updater.check()?.let(::install) }
    }

    /** Called when the app returns to the foreground, e.g. after granting the install permission. */
    fun onResume() {
        (_state.value as? UpdateState.NeedsPermission)?.let { if (updater.canInstall()) install(it.manifest) }
    }

    fun openPermissionSettings() = updater.openInstallPermissionSettings()

    fun retry() {
        (_state.value as? UpdateState.Failed)?.let { install(it.manifest) }
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    private fun install(manifest: UpdateManifest) {
        if (!updater.canInstall()) {
            _state.value = UpdateState.NeedsPermission(manifest)
            return
        }
        _state.value = UpdateState.Downloading(manifest, 0f)
        viewModelScope.launch {
            val ok = updater.downloadAndInstall(manifest) { progress ->
                _state.value = UpdateState.Downloading(manifest, progress)
            }
            // The system installer takes over from here; nothing else to show on success.
            _state.value = if (ok) UpdateState.Idle else UpdateState.Failed(manifest)
        }
    }
}
