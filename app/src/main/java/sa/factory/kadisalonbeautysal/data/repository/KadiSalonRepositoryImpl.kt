package sa.factory.kadisalonbeautysal.data.repository

import sa.factory.kadisalonbeautysal.domain.repository.KadiSalonRepository
import sa.factory.kadisalonbeautysal.hardware.HardwareSensorDriver
import sa.factory.kadisalonbeautysal.model.KadiSalonItem
import sa.factory.kadisalonbeautysal.model.KadiSalonOrder
import sa.factory.kadisalonbeautysal.model.KadiSalonTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KadiSalonRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : KadiSalonRepository {

    private val _catalog = MutableStateFlow<List<KadiSalonItem>>(listOf(
        KadiSalonItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع UV-C Chamber Sanitation IoT Sensor", 180.0, "معتمد", "الخدمات الرئيسية"),
        KadiSalonItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        KadiSalonItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<KadiSalonItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<KadiSalonItem>>(emptyList())
    override val activeCart: StateFlow<List<KadiSalonItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<KadiSalonTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            KadiSalonTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: KadiSalonItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: KadiSalonItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): KadiSalonOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = KadiSalonOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
