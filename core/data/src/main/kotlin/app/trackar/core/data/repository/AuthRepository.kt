package app.trackar.core.data.repository

import app.trackar.core.model.InvalidCredentialsException
import app.trackar.core.model.SessionState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionState: Flow<SessionState>

    /** Fails with [InvalidCredentialsException] when the credentials are rejected. */
    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun logout()
}
