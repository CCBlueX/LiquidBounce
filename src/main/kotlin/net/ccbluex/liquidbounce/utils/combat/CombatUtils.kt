package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.entity.Entity
import net.minecraft.util.math.*
import org.apache.commons.lang3.RandomUtils
import kotlin.math.abs
import kotlin.math.hypot

/**
 * A rotation manager
 */
object CombatManager : Listenable {

    // useful for something like autoSoup
    var pauseCombat: Int = -1

    // useful for something like autopot
    var pauseRotation: Int = -1

    // useful for autoblock
    var pauseBlocking: Int = -1

    private fun updatePauseRotation() {
        if (pauseRotation == -1) return

        pauseRotation--
    }

    private fun updatePauseCombat() {
        if (pauseCombat == -1) return

        pauseCombat--
    }

    private fun updatePauseBlocking() {
        if (pauseBlocking == -1) return

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
}
