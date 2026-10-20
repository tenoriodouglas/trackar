package app.trackar.core.firebase

import app.trackar.core.model.InvalidCredentialsException
import app.trackar.core.model.Session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.tasks.await

interface AuthDataSource {
    /** Emits the current session (or null) and every subsequent change. */
    val session: Flow<Session?>

    suspend fun signIn(email: String, password: String)

    fun signOut()
}

internal class FirebaseAuthDataSource @Inject constructor(private val auth: FirebaseAuth) : AuthDataSource {

    override val session: Flow<Session?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toSession()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.conflate()

    override suspend fun signIn(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw InvalidCredentialsException(e)
        } catch (e: FirebaseAuthInvalidUserException) {
            throw InvalidCredentialsException(e)
        }
    }

    override fun signOut() = auth.signOut()

    private fun FirebaseUser.toSession() = Session(userId = uid, email = email)
}
