package app.trackar.core.data.di

import app.trackar.core.data.repository.AuthRepository
import app.trackar.core.data.repository.DefaultAuthRepository
import app.trackar.core.data.repository.FirestoreVehicleRepository
import app.trackar.core.data.repository.VehicleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindsAuthRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    abstract fun bindsVehicleRepository(impl: FirestoreVehicleRepository): VehicleRepository
}
