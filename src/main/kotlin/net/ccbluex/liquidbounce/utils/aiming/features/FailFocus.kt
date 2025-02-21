package net.ccbluex.liquidbounce.utils.aiming.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.random.Random

/**
 * The fail focus acts as fail rate, it will purposely miss the target on a certain rate.
 */
class FailFocus(owner: EventListener? = null)
    : ToggleableConfigurable(owner, "Fail", false) {

    // Configuration properties
    private val failRate by int("Rate", 3, 1..100, "%")
    val failFactor by float("Factor", 0.04f, 0.01f..0.99f)

    private val strengthHorizontal by floatRange("StrengthHorizontal", 15f..20f, 1f..90f,
        "°")
    private val strengthVertical by floatRange("StrengthVertical", 2f..5f, 0f..90f,
        "°")

    /**
     * The duration it takes to transition from the fail factor to the normal factor.
     */
    private var transitionInDuration by intRange("TransitionInDuration", 1..4, 0..20,
        "ticks")

    // A tick meter to track the duration for which the current target has been focused on
    private var ticksElapsed = 0

    // The currently set transition duration, randomized within the defined range
    private var currentTransitionInDuration = transitionInDuration.random()

    // The shift rotation
    private var shiftRotation = Rotation(0f, 0f)

    val isInFailState: Boolean
        get() = running && ticksElapsed < currentTransitionInDuration

    @Suppress("unused")
    private val gameTick = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        // Fail rate
        val chance = (0f..100f).random()
        if (failRate > chance) {
            currentTransitionInDuration = transitionInDuration.random()
            val yawShift = if (Random.Default.nextBoolean()) {
                strengthHorizontal.random().toFloat()
            } else {
                -strengthHorizontal.random().toFloat()
            }

            val pitchShift = if (Random.Default.nextBoolean()) {
                strengthVertical.random().toFloat()
            } else {
                -strengthVertical.random().toFloat()
            }

            shiftRotation = Rotation(yawShift, pitchShift)
            ticksElapsed = 0

            ModuleDebug.debugParameter(this, "Chance", chance)
            ModuleDebug.debugParameter(this, "Duration", currentTransitionInDuration)
            ModuleDebug.debugParameter(this, "Shift", shiftRotation)
        } else {
            ticksElapsed++
        }

        ModuleDebug.debugParameter(this, "Elapsed", ticksElapsed)
    }

    /**
     * Generates a complete non-sense rotation.
     */
    fun shiftRotation(rotation: Rotation): Rotation {
        val prevRotation = RotationManager.previousRotation ?: return rotation
        val serverRotation = RotationManager.serverRotation

        val deltaYaw = (prevRotation.yaw - serverRotation.yaw) * failFactor
        val deltaPitch = (prevRotation.pitch - serverRotation.pitch) * failFactor

        return Rotation(
            rotation.yaw + deltaYaw + shiftRotation.yaw,
            rotation.pitch + deltaPitch + shiftRotation.pitch
        )
    }

}
