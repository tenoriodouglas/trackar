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

/** Sign-in failures, mapped from the auth provider so the UI can explain them. */
sealed class AuthException(message: String, cause: Throwable?) : Exception(message, cause)

/** The backend rejected the e-mail/password pair. */
class InvalidCredentialsException(cause: Throwable? = null) : AuthException("Invalid credentials", cause)

/** No connectivity or the auth service is unreachable. */
class NetworkUnavailableException(cause: Throwable? = null) : AuthException("Network unavailable", cause)

/** Temporarily blocked after too many attempts. */
class TooManyAttemptsException(cause: Throwable? = null) : AuthException("Too many attempts", cause)

/** Any other provider error; [code] is shown to help diagnose configuration issues. */
class AuthServiceException(val code: String, cause: Throwable? = null) : AuthException("Auth error: $code", cause)
