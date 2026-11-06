package sa.factory.kadisalonbeautysal.di

import sa.factory.kadisalonbeautysal.data.repository.KadiSalonRepositoryImpl
import sa.factory.kadisalonbeautysal.domain.repository.KadiSalonRepository
import sa.factory.kadisalonbeautysal.domain.usecase.*
import sa.factory.kadisalonbeautysal.hardware.HardwareSensorDriver
import sa.factory.kadisalonbeautysal.ui.viewmodel.KadiSalonViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: KadiSalonRepository = KadiSalonRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): KadiSalonViewModel = KadiSalonViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
