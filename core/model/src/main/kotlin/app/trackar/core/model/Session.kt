package app.trackar.core.model

/** Authenticated user. */
data class Session(val userId: String, val email: String?)

sealed interface SessionState {
    /** Auth state not resolved yet (app start). */
    data object Loading : SessionState
    data class Loaded(val session: Session?) : SessionState
}

/** Real-time stream state: CONNECTED means data is confirmed by the server, not just cached. */
enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED }

/** Thrown when the backend rejects the user's credentials. */
class InvalidCredentialsException(cause: Throwable? = null) : Exception("Invalid credentials", cause)
