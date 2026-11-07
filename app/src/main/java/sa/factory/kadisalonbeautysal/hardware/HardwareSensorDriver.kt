package sa.factory.kadisalonbeautysal.hardware

import sa.factory.kadisalonbeautysal.engine.SalonServiceCategory
import sa.factory.kadisalonbeautysal.engine.SalonSpecialistBookingEngine
import sa.factory.kadisalonbeautysal.engine.UvcCycleType
import sa.factory.kadisalonbeautysal.engine.UvcSanitationProtocolEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver(
    val uvcEngine: UvcSanitationProtocolEngine = UvcSanitationProtocolEngine(),
    val bookingEngine: SalonSpecialistBookingEngine = SalonSpecialistBookingEngine()
) {
    private val peripheral = "UV-C Chamber Sanitation IoT Sensor"

    init {
        // Pre-warm sample simulation data
        uvcEngine.startCycle(UvcCycleType.FAST_SANITIZATION, "OPR-DEFAULT")
        repeat(120) { uvcEngine.tickSecond() }
        bookingEngine.bookAppointment("سارة عبد العزيز", "SPEC-01", "STATION-HAIR-01", SalonServiceCategory.HAIR_STYLING, 14)
        bookingEngine.bookAppointment("نوف العتيبي", "SPEC-02", "SUITE-FACIAL-01", SalonServiceCategory.FACIAL_SKINCARE, 15)
    }

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "Handshake initializing..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "UV-C Chamber Sanitation IoT Sensor: ONLINE | 254nm ACTIVE"))

        while (true) {
            delay(1500)
            uvcEngine.tickSecond()
            val state = uvcEngine.getChamberState()
            val occupancy = bookingEngine.getStationOccupancyRate(8)
            val dosage = uvcEngine.calculateDosageMilliJoules(120, 1200.0)
            val elimPct = uvcEngine.calculatePathogenElimination(dosage)
            val jitter = Random.nextInt(1, 8)

            val payload = "UV-C State: $state | Dose: ${String.format("%.1f", dosage)} mJ/cm2 | Kill: ${String.format("%.2f", elimPct)}% | Salon Occupancy: ${String.format("%.0f", occupancy)}% (+${jitter}ms)"
            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
