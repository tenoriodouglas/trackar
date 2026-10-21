package app.trackar.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Injectable time source so date logic is testable. */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    fun providesClock(): Clock = Clock.systemDefaultZone()
}
