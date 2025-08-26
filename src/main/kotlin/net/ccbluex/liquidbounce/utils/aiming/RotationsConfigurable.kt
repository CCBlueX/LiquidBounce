package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.features.processors.FailRotationProcessor
import net.ccbluex.liquidbounce.utils.aiming.features.processors.ShortStopRotationProcessor
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.*
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction
import net.minecraft.entity.Entity

/**
 * Configurable to configure the dynamic rotation engine
 */
open class RotationsConfigurable(
    owner: EventListener,
    movementCorrection: MovementCorrection = MovementCorrection.SILENT,
    combatSpecific: Boolean = false
) : Configurable("Rotations") {

    private val angleSmooth = choices(owner, "AngleSmooth", 0) {
        val linearAngleSmooth = LinearAngleSmooth(it)
        val interpolationAngleSmooth = if (combatSpecific) InterpolationAngleSmooth(it) else null

        listOfNotNull(
            linearAngleSmooth,
            SigmoidAngleSmooth(it),
            interpolationAngleSmooth,
            AccelerationAngleSmooth(it),
            if (combatSpecific) MinaraiAngleSmooth(it, interpolationAngleSmooth ?: linearAngleSmooth) else null
        ).toTypedArray()
    }

    private var shortStop = ShortStopRotationProcessor(owner).takeIf { combatSpecific }?.also { tree(it) }
    private val fail = FailRotationProcessor(owner).takeIf { combatSpecific }?.also { tree(it) }

    private val movementCorrection by enumChoice("MovementCorrection", movementCorrection)
    private val resetThreshold by float("ResetThreshold", 2f, 1f..180f)
    private val ticksUntilReset by int("TicksUntilReset", 5, 1..30, "ticks")

    fun toRotationTarget(
        rotation: Rotation,
        entity: Entity? = null,
        considerInventory: Boolean = false,
        whenReached: RestrictedSingleUseAction? = null
    ) = RotationTarget(
        rotation,
        entity,
        listOfNotNull(
            angleSmooth.activeChoice,
            fail.takeIf { failFocus -> failFocus?.running == true },
            shortStop.takeIf { shortStop -> shortStop?.running == true }
        ),
        ticksUntilReset,
        resetThreshold,
        considerInventory,
        movementCorrection,
        whenReached
    )

    /**
     * How long it takes to rotate to a rotation in ticks
     *
     * Calculates the difference from the server rotation to the target rotation and divides it by the
     * minimum turn speed (to make sure we are always there in time)
     *
     * @param rotation The rotation to rotate to
     * @return The amount of ticks it takes to rotate to the rotation
     */
    fun calculateTicks(rotation: Rotation) = angleSmooth.activeChoice
        .calculateTicks(RotationManager.actualServerRotation, rotation)

}
