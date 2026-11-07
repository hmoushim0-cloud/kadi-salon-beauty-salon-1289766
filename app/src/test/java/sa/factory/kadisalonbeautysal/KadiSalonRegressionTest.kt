package sa.factory.kadisalonbeautysal

import sa.factory.kadisalonbeautysal.data.repository.KadiSalonRepositoryImpl
import sa.factory.kadisalonbeautysal.engine.SalonServiceCategory
import sa.factory.kadisalonbeautysal.engine.SalonSpecialistBookingEngine
import sa.factory.kadisalonbeautysal.engine.SpecialistStatus
import sa.factory.kadisalonbeautysal.engine.UvcChamberState
import sa.factory.kadisalonbeautysal.engine.UvcCycleType
import sa.factory.kadisalonbeautysal.engine.UvcSanitationProtocolEngine
import sa.factory.kadisalonbeautysal.hardware.HardwareSensorDriver
import sa.factory.kadisalonbeautysal.model.KadiSalonItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KadiSalonRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = KadiSalonRepositoryImpl(sensorDriver)

        val item1 = KadiSalonItem("t1", "خدمة تجريبية أ", "الوصف", 100.0, "معتمد", "عام")
        val item2 = KadiSalonItem("t2", "خدمة تجريبية ب", "الوصف", 200.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testUvcEngineCycleLifecycleAndCompletion() {
        val engine = UvcSanitationProtocolEngine()
        assertEquals(UvcChamberState.IDLE, engine.getChamberState())
        assertEquals(254.0, engine.wavelengthNanometers, 0.01)

        val started = engine.startCycle(UvcCycleType.FAST_SANITIZATION)
        assertTrue(started)
        assertEquals(UvcChamberState.SANITIZING, engine.getChamberState())

        repeat(180) { engine.tickSecond() }
        assertEquals(UvcChamberState.CYCLE_COMPLETED, engine.getChamberState())

        val history = engine.getAuditHistory()
        assertEquals(1, history.size)
        assertEquals(UvcChamberState.CYCLE_COMPLETED, history.first().finalState)
        assertEquals(180, history.first().durationSeconds)
    }

    @Test
    fun testUvcEngineInterlockSafetyDoorAbort() {
        val engine = UvcSanitationProtocolEngine()
        engine.startCycle(UvcCycleType.DEEP_STERILIZATION)
        assertEquals(UvcChamberState.SANITIZING, engine.getChamberState())

        // Simulating chamber door opened during irradiation
        val safetyResult = engine.setDoorInterlock(isClosed = false)
        assertFalse(safetyResult)
        assertEquals(UvcChamberState.INTERLOCK_ABORTED, engine.getChamberState())

        val history = engine.getAuditHistory()
        assertEquals(1, history.size)
        assertEquals(UvcChamberState.INTERLOCK_ABORTED, history.first().finalState)
    }

    @Test
    fun testUvcEngineDosageMathCalculation() {
        val engine = UvcSanitationProtocolEngine()
        // 1200 uW/cm2 for 180 seconds = (1200 * 180) / 1000 = 216 mJ/cm2
        val dosageFast = engine.calculateDosageMilliJoules(180, 1200.0)
        assertEquals(216.0, dosageFast, 0.001)

        // 2400 uW/cm2 for 600 seconds = (2400 * 600) / 1000 = 1440 mJ/cm2
        val dosageDeep = engine.calculateDosageMilliJoules(600, 2400.0)
        assertEquals(1440.0, dosageDeep, 0.001)
    }

    @Test
    fun testUvcPathogenEliminationKinetics() {
        val engine = UvcSanitationProtocolEngine()
        // Fast cycle dosage (216 mJ/cm2) should provide log-4+ reduction (> 99.99%)
        val elimination = engine.calculatePathogenElimination(216.0)
        assertTrue("Elimination must be > 99.99%", elimination >= 99.99)
        assertTrue("Elimination cannot exceed 100%", elimination <= 100.0)
    }

    @Test
    fun testUvcChamberResetAndIdleState() {
        val engine = UvcSanitationProtocolEngine()
        engine.startCycle(UvcCycleType.FAST_SANITIZATION)
        repeat(180) { engine.tickSecond() }
        assertEquals(UvcChamberState.CYCLE_COMPLETED, engine.getChamberState())

        engine.resetChamber()
        assertEquals(UvcChamberState.IDLE, engine.getChamberState())
    }

    @Test
    fun testSalonSpecialistRosterPreload() {
        val bookingEngine = SalonSpecialistBookingEngine()
        val specialists = bookingEngine.getSpecialists()
        assertEquals(4, specialists.size)

        val spec1 = bookingEngine.findSpecialistById("SPEC-01")
        assertNotNull(spec1)
        assertEquals(SalonServiceCategory.HAIR_STYLING, spec1?.primaryCategory)
        assertTrue(spec1!!.ratingScore >= 4.9)
    }

    @Test
    fun testSalonBookingSuccessAndOccupancy() {
        val bookingEngine = SalonSpecialistBookingEngine()
        val apt = bookingEngine.bookAppointment(
            customerName = "العنود السديري",
            specialistId = "SPEC-01",
            stationId = "STATION-HAIR-01",
            category = SalonServiceCategory.HAIR_STYLING,
            startHour = 16
        )

        assertNotNull(apt)
        assertEquals("CONFIRMED", apt?.status)
        assertEquals(1, bookingEngine.getAppointments().size)

        val occupancy = bookingEngine.getStationOccupancyRate(totalStations = 4)
        assertEquals(25.0, occupancy, 0.01)
    }

    @Test
    fun testSalonBookingCollisionPrevention() {
        val bookingEngine = SalonSpecialistBookingEngine()
        val apt1 = bookingEngine.bookAppointment(
            customerName = "منى الحربي",
            specialistId = "SPEC-02",
            stationId = "SUITE-FACIAL-01",
            category = SalonServiceCategory.FACIAL_SKINCARE,
            startHour = 17
        )
        assertNotNull(apt1)

        // Collision attempt on same specialist & hour
        val aptConflict = bookingEngine.bookAppointment(
            customerName = "هند العبدالله",
            specialistId = "SPEC-02",
            stationId = "SUITE-FACIAL-02",
            category = SalonServiceCategory.FACIAL_SKINCARE,
            startHour = 17
        )
        assertNull("Should reject collision on busy specialist", aptConflict)
    }

    @Test
    fun testHardwareSensorDriverEngineTelemetryIntegration() = runBlocking {
        val driver = HardwareSensorDriver()
        val frames = driver.observePeripheralStream().take(2).toList()

        assertEquals("CONNECTING", frames[0].connectionState)
        assertEquals("ONLINE", frames[1].connectionState)
        assertTrue(frames[1].livePayload.contains("UV-C Chamber Sanitation"))
    }
}
