package sa.factory.kadisalonbeautysal.engine

enum class SalonServiceCategory(val arabicLabel: String, val baseDurationMinutes: Int) {
    HAIR_STYLING("تصفيف وعناية الشعر الملكي", 45),
    FACIAL_SKINCARE("جلسات نضارة البشرة والهيدرافيشل", 60),
    MOROCCAN_BATH("الحمام المغربي الملكي بالأعشاب", 75),
    VIP_NAIL_CARE("العناية الماسية بالأظافر والسبا", 40)
}

enum class SpecialistStatus {
    AVAILABLE,
    IN_SESSION,
    ON_BREAK,
    OFFLINE
}

data class SalonSpecialist(
    val id: String,
    val name: String,
    val primaryCategory: SalonServiceCategory,
    val ratingScore: Double,
    val totalCompletedSessions: Int,
    val status: SpecialistStatus = SpecialistStatus.AVAILABLE
)

data class SalonAppointment(
    val appointmentId: String,
    val customerName: String,
    val specialistId: String,
    val category: SalonServiceCategory,
    val stationId: String,
    val startHour: Int, // 24h format (e.g., 14 for 14:00)
    val durationMinutes: Int,
    val status: String = "CONFIRMED"
)

class SalonSpecialistBookingEngine {
    private val specialists = mutableListOf(
        SalonSpecialist("SPEC-01", "أخصائية سارة القحطاني", SalonServiceCategory.HAIR_STYLING, 4.95, 342),
        SalonSpecialist("SPEC-02", "أخصائية نورة الشمري", SalonServiceCategory.FACIAL_SKINCARE, 4.98, 419),
        SalonSpecialist("SPEC-03", "أخصائية مريم الدوسري", SalonServiceCategory.MOROCCAN_BATH, 4.92, 280),
        SalonSpecialist("SPEC-04", "أخصائية ريم المطيري", SalonServiceCategory.VIP_NAIL_CARE, 4.96, 512)
    )

    private val appointments = mutableListOf<SalonAppointment>()

    fun getSpecialists(): List<SalonSpecialist> = specialists.toList()
    fun getAppointments(): List<SalonAppointment> = appointments.toList()

    fun findSpecialistById(id: String): SalonSpecialist? = specialists.find { it.id == id }

    fun isSlotAvailable(specialistId: String, stationId: String, startHour: Int): Boolean {
        return appointments.none { apt ->
            (apt.specialistId == specialistId || apt.stationId == stationId) &&
                    apt.startHour == startHour &&
                    apt.status == "CONFIRMED"
        }
    }

    fun bookAppointment(
        customerName: String,
        specialistId: String,
        stationId: String,
        category: SalonServiceCategory,
        startHour: Int
    ): SalonAppointment? {
        val specialist = findSpecialistById(specialistId) ?: return null
        if (!isSlotAvailable(specialistId, stationId, startHour)) {
            return null
        }

        val appointment = SalonAppointment(
            appointmentId = "APT-${System.currentTimeMillis() % 100000}",
            customerName = customerName,
            specialistId = specialist.id,
            category = category,
            stationId = stationId,
            startHour = startHour,
            durationMinutes = category.baseDurationMinutes,
            status = "CONFIRMED"
        )
        appointments.add(appointment)
        return appointment
    }

    fun getStationOccupancyRate(totalStations: Int = 8): Double {
        val activeCount = appointments.count { it.status == "CONFIRMED" }
        if (totalStations == 0) return 0.0
        val rate = (activeCount.toDouble() / totalStations.toDouble()) * 100.0
        return if (rate > 100.0) 100.0 else rate
    }

    fun updateSpecialistStatus(id: String, status: SpecialistStatus): Boolean {
        val index = specialists.indexOfFirst { it.id == id }
        if (index != -1) {
            specialists[index] = specialists[index].copy(status = status)
            return true
        }
        return false
    }
}
