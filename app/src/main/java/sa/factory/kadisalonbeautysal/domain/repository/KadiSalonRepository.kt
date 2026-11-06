package sa.factory.kadisalonbeautysal.domain.repository

import sa.factory.kadisalonbeautysal.model.KadiSalonItem
import sa.factory.kadisalonbeautysal.model.KadiSalonOrder
import sa.factory.kadisalonbeautysal.model.KadiSalonTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface KadiSalonRepository {
    val catalogFlow: Flow<List<KadiSalonItem>>
    val activeCart: StateFlow<List<KadiSalonItem>>
    val telemetryFlow: Flow<KadiSalonTelemetry>
    
    fun addToCart(item: KadiSalonItem)
    fun removeFromCart(item: KadiSalonItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): KadiSalonOrder
    suspend fun refreshCatalog()
}
