package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler

/**
 * A rotation manager
 */
object CombatManager : Listenable {

    // useful for something like autoSoup
    private var pauseCombat: Int = 0

    // useful for something like autopot
    private var pauseRotation: Int = 0

    // useful for autoblock
    private var pauseBlocking: Int = 0

    private fun updatePauseRotation() {
        if (pauseRotation <= 0) return

        pauseRotation--
    }

    private fun updatePauseCombat() {
        if (pauseCombat <= 0) return

        pauseCombat--
    }

    private fun updatePauseBlocking() {
        if (pauseBlocking <= 0) return

        pauseBlocking--
    }

    /**
     * Update current rotation to new rotation step
     */
    fun update() {
        updatePauseRotation()
        updatePauseCombat()
        // TODO: implement this for killaura autoblock and other
        updatePauseBlocking()
    }

    val tickHandler = handler<GameTickEvent> {
        update()
    }

    fun shouldPauseCombat(): Boolean = this.pauseCombat > 0
    fun shouldPauseRotation(): Boolean = this.pauseRotation > 0
    fun shouldPauseBlocking(): Boolean = this.pauseBlocking > 0

    fun pauseCombatForAtLeast(pauseTime: Int) {
        this.pauseCombat = this.pauseCombat.coerceAtLeast(pauseTime)
    }

    fun pauseRotationForAtLeast(pauseTime: Int) {
        this.pauseRotation = this.pauseRotation.coerceAtLeast(pauseTime)
    }

    fun pauseBlockingForAtLeast(pauseTime: Int) {
        this.pauseBlocking = this.pauseBlocking.coerceAtLeast(pauseTime)
    }
}
