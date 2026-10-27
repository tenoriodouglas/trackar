package app.trackar.core.firebase.di

import app.trackar.core.firebase.AuthDataSource
import app.trackar.core.firebase.CallableVehicleActionsDataSource
import app.trackar.core.firebase.FirebaseAuthDataSource
import app.trackar.core.firebase.FirestoreVehicleDataSource
import app.trackar.core.firebase.VehicleActionsDataSource
import app.trackar.core.firebase.VehicleDataSource
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FirebaseModule {

    @Binds
    abstract fun bindsAuthDataSource(impl: FirebaseAuthDataSource): AuthDataSource

    @Binds
    abstract fun bindsVehicleDataSource(impl: FirestoreVehicleDataSource): VehicleDataSource

    @Binds
    abstract fun bindsVehicleActionsDataSource(impl: CallableVehicleActionsDataSource): VehicleActionsDataSource

    companion object {
        @Provides
        @Singleton
        fun providesFirebaseAuth(): FirebaseAuth = Firebase.auth

        // Offline persistence is on by default: the Firestore cache is the app's local store.
        @Provides
        @Singleton
        fun providesFirestore(): FirebaseFirestore = Firebase.firestore

        /** Must match the functions' REGION parameter (backend/functions/.env). */
        @Provides
        @Singleton
        fun providesFunctions(): FirebaseFunctions = Firebase.functions(FUNCTIONS_REGION)

        private const val FUNCTIONS_REGION = "southamerica-east1"
    }
}
