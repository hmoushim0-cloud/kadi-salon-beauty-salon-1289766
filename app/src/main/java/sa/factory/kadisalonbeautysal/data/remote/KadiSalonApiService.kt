package sa.factory.kadisalonbeautysal.data.remote

import sa.factory.kadisalonbeautysal.model.KadiSalonItem
import sa.factory.kadisalonbeautysal.model.KadiSalonOrder

interface KadiSalonApiService {
    suspend fun fetchCatalog(): List<KadiSalonItem>
    suspend fun submitOrder(order: KadiSalonOrder): Boolean
}
