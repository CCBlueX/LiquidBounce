package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.utils.aiming.data.AngleLine

open class RotationEngine(
    val owner: Listenable,
    combatSpecific: Boolean = false
) : Configurable("Rotations") {

    val movementCorrectionMode by enumChoice("MovementCorrection", MovementCorrectionMode.SILENT)
    val ticksUntilReset by int("TicksUntilReset", 5, 1..30, "ticks")

    fun ticksUntil(angle: AngleLine) = 0

    enum class MovementCorrectionMode(override val choiceName: String, var changeLook: Boolean) : NamedChoice {
        NONE("None", false),
        SILENT("Silent", false),
        STRICT("Strict", false),
        CHANGE_LOOK("ChangeLook", true),
    }

}
