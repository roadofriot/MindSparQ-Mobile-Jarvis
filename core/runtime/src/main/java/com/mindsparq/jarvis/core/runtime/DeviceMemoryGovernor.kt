package com.mindsparq.jarvis.core.runtime

class DeviceMemoryGovernor {

    fun isMemoryConstrained(availableMb: Long, thresholdMb: Long = 500L): Boolean {
        return availableMb < thresholdMb
    }

    fun recommendedWorkerConcurrency(availableMb: Long): Int {
        return when {
            availableMb >= 3000L -> 4
            availableMb >= 1500L -> 2
            else -> 1 // Constrained host: clamp to single worker
        }
    }

    fun shouldThrottleThermal(thermalStatus: Int): Boolean {
        // android.os.PowerManager.THERMAL_STATUS_SEVERE is 4
        // android.os.PowerManager.THERMAL_STATUS_CRITICAL is 5
        return thermalStatus >= 4
    }
}

