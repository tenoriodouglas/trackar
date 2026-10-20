package app.trackar.core.firebase.di

import app.trackar.core.firebase.AuthDataSource
import app.trackar.core.firebase.FirebaseAuthDataSource
import app.trackar.core.firebase.FirestoreVehicleDataSource
import app.trackar.core.firebase.VehicleDataSource
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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

    companion object {
        @Provides
        @Singleton
        fun providesFirebaseAuth(): FirebaseAuth = Firebase.auth

        // Offline persistence is on by default: the Firestore cache is the app's local store.
        @Provides
        @Singleton
        fun providesFirestore(): FirebaseFirestore = Firebase.firestore
    }
}
