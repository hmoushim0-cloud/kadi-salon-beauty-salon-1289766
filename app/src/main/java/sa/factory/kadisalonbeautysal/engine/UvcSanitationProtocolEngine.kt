package sa.factory.kadisalonbeautysal.engine

import kotlin.math.exp
import kotlin.math.min

enum class UvcCycleType(val durationSeconds: Int, val targetIntensityMicroWatts: Double) {
    FAST_SANITIZATION(180, 1200.0),
    DEEP_STERILIZATION(600, 2400.0)
}

enum class UvcChamberState {
    IDLE,
    DOOR_OPEN,
    SANITIZING,
    CYCLE_COMPLETED,
    INTERLOCK_ABORTED,
    FAULT
}

data class SanitationAuditRecord(
    val cycleId: String,
    val cycleType: UvcCycleType,
    val durationSeconds: Int,
    val deliveredDosageMilliJoules: Double,
    val pathogenEliminationPct: Double,
    val operatorId: String,
    val finalState: UvcChamberState,
    val timestamp: Long = System.currentTimeMillis()
)

class UvcSanitationProtocolEngine {
    private var isDoorInterlockEngaged: Boolean = true // true = door safely closed
    private var currentState: UvcChamberState = UvcChamberState.IDLE
    private var activeCycleType: UvcCycleType? = null
    private var elapsedSeconds: Int = 0
    private val auditLogs = mutableListOf<SanitationAuditRecord>()

    val wavelengthNanometers: Double = 254.0

    fun getChamberState(): UvcChamberState = currentState
    fun isDoorClosed(): Boolean = isDoorInterlockEngaged
    fun getAuditHistory(): List<SanitationAuditRecord> = auditLogs.toList()

    fun setDoorInterlock(isClosed: Boolean): Boolean {
        isDoorInterlockEngaged = isClosed
        if (!isClosed && currentState == UvcChamberState.SANITIZING) {
            currentState = UvcChamberState.INTERLOCK_ABORTED
            recordAudit(currentState)
            return false
        }
        if (!isClosed && currentState == UvcChamberState.IDLE) {
            currentState = UvcChamberState.DOOR_OPEN
        } else if (isClosed && currentState == UvcChamberState.DOOR_OPEN) {
            currentState = UvcChamberState.IDLE
        }
        return true
    }

    fun startCycle(type: UvcCycleType, operatorId: String = "TECH-01"): Boolean {
        if (!isDoorInterlockEngaged) {
            currentState = UvcChamberState.INTERLOCK_ABORTED
            return false
        }
        activeCycleType = type
        elapsedSeconds = 0
        currentState = UvcChamberState.SANITIZING
        return true
    }

    fun tickSecond(): UvcChamberState {
        if (currentState != UvcChamberState.SANITIZING) {
            return currentState
        }
        if (!isDoorInterlockEngaged) {
            currentState = UvcChamberState.INTERLOCK_ABORTED
            recordAudit(currentState)
            return currentState
        }

        elapsedSeconds++
        val target = activeCycleType?.durationSeconds ?: 180
        if (elapsedSeconds >= target) {
            currentState = UvcChamberState.CYCLE_COMPLETED
            recordAudit(currentState)
        }
        return currentState
    }

    fun calculateDosageMilliJoules(durationSeconds: Int, intensityMicroWatts: Double): Double {
        // Dosage (mJ/cm2) = (Intensity uW/cm2 * Time s) / 1000
        return (intensityMicroWatts * durationSeconds) / 1000.0
    }

    fun calculatePathogenElimination(dosageMilliJoules: Double): Double {
        // Mathematical first-order Chick-Watson inactivation kinetics
        // Standard UV-C inactivation rate constant k ~ 0.28 cm2/mJ for salon pathogens (Staph, Candida, Pseudomonas)
        val k = 0.28
        val survivalFraction = exp(-k * dosageMilliJoules)
        val eliminationPct = (1.0 - survivalFraction) * 100.0
        return min(99.9999, eliminationPct)
    }

    private fun recordAudit(finalState: UvcChamberState) {
        val cycle = activeCycleType ?: UvcCycleType.FAST_SANITIZATION
        val dosage = calculateDosageMilliJoules(elapsedSeconds, cycle.targetIntensityMicroWatts)
        val elimination = calculatePathogenElimination(dosage)
        val record = SanitationAuditRecord(
            cycleId = "UVC-${System.currentTimeMillis() % 100000}",
            cycleType = cycle,
            durationSeconds = elapsedSeconds,
            deliveredDosageMilliJoules = dosage,
            pathogenEliminationPct = elimination,
            operatorId = "OPR-KADI-SA",
            finalState = finalState
        )
        auditLogs.add(record)
    }

    fun resetChamber() {
        elapsedSeconds = 0
        activeCycleType = null
        currentState = if (isDoorInterlockEngaged) UvcChamberState.IDLE else UvcChamberState.DOOR_OPEN
    }
}
