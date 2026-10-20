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

enum class LoginError { INVALID_CREDENTIALS, NETWORK }
