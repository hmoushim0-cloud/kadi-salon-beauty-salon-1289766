package sa.factory.kadisalonbeautysal.domain.usecase

import sa.factory.kadisalonbeautysal.domain.repository.KadiSalonRepository
import sa.factory.kadisalonbeautysal.model.KadiSalonItem
import sa.factory.kadisalonbeautysal.model.KadiSalonOrder
import sa.factory.kadisalonbeautysal.model.KadiSalonTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: KadiSalonRepository) {
    operator fun invoke(): Flow<List<KadiSalonItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: KadiSalonRepository) {
    val cart = repository.activeCart
    fun add(item: KadiSalonItem) = repository.addToCart(item)
    fun remove(item: KadiSalonItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: KadiSalonRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): KadiSalonOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: KadiSalonRepository) {
    operator fun invoke(): Flow<KadiSalonTelemetry> = repository.telemetryFlow
}
