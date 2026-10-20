package app.trackar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trackar.core.designsystem.theme.TrackarTheme
import app.trackar.core.model.SessionState
import app.trackar.ui.TrackarApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hold the splash until we know whether a session exists, avoiding a login-screen flash.
        splashScreen.setKeepOnScreenCondition { viewModel.sessionState.value is SessionState.Loading }

        setContent {
            val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
            TrackarTheme {
                TrackarApp(sessionState)
            }
        }
    }
}
