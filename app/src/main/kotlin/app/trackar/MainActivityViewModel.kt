package app.trackar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trackar.core.data.repository.AuthRepository
import app.trackar.core.model.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainActivityViewModel @Inject constructor(authRepository: AuthRepository) : ViewModel() {
    val sessionState: StateFlow<SessionState> = authRepository.sessionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
}
