package app.trackar.feature.auth

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: LoginError? = null
) {
    val canSubmit: Boolean
        get() = !isLoading && email.isNotBlank() && password.isNotBlank()
}

sealed interface LoginError {
    data object InvalidCredentials : LoginError
    data object Network : LoginError
    data object TooManyAttempts : LoginError

    /** Unexpected provider error; the code is shown to help support diagnose it. */
    data class Service(val code: String) : LoginError
}
