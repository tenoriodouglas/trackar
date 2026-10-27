package app.trackar.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trackar.core.data.repository.AuthRepository
import app.trackar.core.model.AuthServiceException
import app.trackar.core.model.InvalidCredentialsException
import app.trackar.core.model.TooManyAttemptsException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Non-sensitive fields survive process death; the password never does.
    private val _uiState = MutableStateFlow(
        LoginUiState(
            email = savedStateHandle[KEY_EMAIL] ?: ""
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        savedStateHandle[KEY_EMAIL] = value
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.login(state.email, state.password)
            _uiState.update { current ->
                result.fold(
                    onSuccess = { current.copy(isLoading = false, password = "") },
                    onFailure = { current.copy(isLoading = false, error = it.toLoginError()) }
                )
            }
        }
    }

    private fun Throwable.toLoginError(): LoginError = when (this) {
        is InvalidCredentialsException -> LoginError.InvalidCredentials
        is TooManyAttemptsException -> LoginError.TooManyAttempts
        is AuthServiceException -> LoginError.Service(code)
        else -> LoginError.Network
    }

    private companion object {
        const val KEY_EMAIL = "email"
    }
}
