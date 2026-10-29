package com.mindsparq.jarvis.core.runtime

import android.content.Context
import android.os.PowerManager
import java.util.concurrent.atomic.AtomicBoolean

interface WakeLockController {
    fun acquire(tag: String, timeoutMs: Long = 10 * 60 * 1000L)
    fun release()
    fun isHeld(): Boolean
}

class AndroidWakeLockController(private val context: Context) : WakeLockController {
    private var wakeLock: PowerManager.WakeLock? = null
    private val held = AtomicBoolean(false)

    @Synchronized
    override fun acquire(tag: String, timeoutMs: Long) {
        if (held.get()) return
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        val lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MindSparQJarvis:$tag")
        lock.setReferenceCounted(false)
        lock.acquire(timeoutMs)
        wakeLock = lock
        held.set(true)
    }

    @Synchronized
    override fun release() {
        if (!held.get()) return
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        held.set(false)
    }

    override fun isHeld(): Boolean = held.get()
}

class TestWakeLockController : WakeLockController {
    private val held = AtomicBoolean(false)
    var lastTag: String? = null
        private set

    override fun acquire(tag: String, timeoutMs: Long) {
        lastTag = tag
        held.set(true)
    }

    override fun release() {
        held.set(false)
    }

    override fun isHeld(): Boolean = held.get()
}

