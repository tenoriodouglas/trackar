package app.trackar.feature.auth

import androidx.lifecycle.SavedStateHandle
import app.trackar.core.model.AuthServiceException
import app.trackar.core.model.InvalidCredentialsException
import app.trackar.core.testing.MainDispatcherRule
import app.trackar.core.testing.TestAuthRepository
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: TestAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        authRepository = TestAuthRepository()
        viewModel = LoginViewModel(authRepository, SavedStateHandle())
    }

    @Test
    fun `submit is disabled until all fields are filled`() {
        assertFalse(viewModel.uiState.value.canSubmit)
        fillForm()
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `successful login clears password and error`() = runTest {
        fillForm()
        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals("a@b.com" to "secret", authRepository.lastLogin)
        assertEquals("", state.password)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `network failure is surfaced`() = runTest {
        authRepository.loginResult = Result.failure(IOException())
        fillForm()
        viewModel.submit()

        assertEquals(LoginError.Network, viewModel.uiState.value.error)
    }

    @Test
    fun `provider error code is surfaced`() = runTest {
        authRepository.loginResult = Result.failure(AuthServiceException("ERROR_INTERNAL_ERROR"))
        fillForm()
        viewModel.submit()

        assertEquals(LoginError.Service("ERROR_INTERNAL_ERROR"), viewModel.uiState.value.error)
    }

    @Test
    fun `invalid credentials are surfaced`() = runTest {
        authRepository.loginResult = Result.failure(InvalidCredentialsException())
        fillForm()
        viewModel.submit()

        assertEquals(LoginError.InvalidCredentials, viewModel.uiState.value.error)
    }

    @Test
    fun `editing a field clears the error`() = runTest {
        authRepository.loginResult = Result.failure(IOException())
        fillForm()
        viewModel.submit()
        viewModel.onPasswordChange("other")

        assertNull(viewModel.uiState.value.error)
    }

    private fun fillForm() {
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("secret")
    }
}
