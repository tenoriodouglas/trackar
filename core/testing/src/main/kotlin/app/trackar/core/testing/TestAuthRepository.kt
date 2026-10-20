package app.trackar.core.testing

import app.trackar.core.data.repository.AuthRepository
import app.trackar.core.model.Session
import app.trackar.core.model.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TestAuthRepository : AuthRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.Loaded(null))
    override val sessionState: StateFlow<SessionState> = state

    /** Result returned by the next [login] call. */
    var loginResult: Result<Unit> = Result.success(Unit)
    var lastLogin: Pair<String, String>? = null
        private set

    override suspend fun login(email: String, password: String): Result<Unit> {
        lastLogin = email to password
        loginResult.onSuccess { state.value = SessionState.Loaded(Session(userId = "uid", email = email)) }
        return loginResult
    }

    override suspend fun logout() {
        state.value = SessionState.Loaded(null)
    }
}
