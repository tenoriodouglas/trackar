package app.trackar.core.data.repository

import app.trackar.core.common.result.suspendRunCatching
import app.trackar.core.firebase.AuthDataSource
import app.trackar.core.model.SessionState
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal class DefaultAuthRepository @Inject constructor(private val authDataSource: AuthDataSource) : AuthRepository {

    override val sessionState: Flow<SessionState> = authDataSource.session
        .map<_, SessionState> { SessionState.Loaded(it) }
        .onStart { emit(SessionState.Loading) }

    override suspend fun login(email: String, password: String): Result<Unit> = suspendRunCatching {
        authDataSource.signIn(email.trim(), password)
    }

    override suspend fun logout() = authDataSource.signOut()
}
