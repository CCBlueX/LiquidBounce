package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.random.Random

/**
 * The fail focus acts as fail rate, it will purposely miss the target on a certain rate.
 */
class FailFocus(owner: Listenable? = null)
    : ToggleableConfigurable(owner, "Fail", false) {

    // Configuration properties
    private val failRate by int("Rate", 4, 1..100, "%")
    val failFactor by float("Factor", 0.02f, 0.02f..0.2f)

    private val strengthHorizontal by floatRange("StrengthHorizontal", 15f..30f, 1f..90f,
        "°")
    private val strengthVertical by floatRange("StrengthVertical", 2f..5f, 0f..90f,
        "°")

    /**
     * The duration it takes to transition from the fail factor to the normal factor.
     */
    private var transitionInDuration by intRange("TransitionInDuration", 50..200, 0..500,
        "ms")

    // A chronometer to track the duration for which the current target has been focused on
    private val failChronometer = Chronometer()

    // The currently set transition duration, randomized within the defined range
    private var currentTransitionInDuration = transitionInDuration.random()

    // The shift rotation
    private var shiftRotation = Rotation(0f, 0f)

    val isInFailState: Boolean
        get() = failChronometer.elapsed < currentTransitionInDuration

    @Suppress("unused")
    private val gameTick = handler<GameTickEvent> {
        ModuleDebug.debugParameter(this, "Elapsed", failChronometer.elapsed)

        // Fail rate
        val chance = (0f..100f).random()
        if (failRate > chance) {
            currentTransitionInDuration = transitionInDuration.random()
            val yawShift = if (Random.nextBoolean()) {
                strengthHorizontal.random().toFloat()
            } else {
                -strengthHorizontal.random().toFloat()
            }

            val pitchShift = if (Random.nextBoolean()) {
                strengthVertical.random().toFloat()
            } else {
                -strengthVertical.random().toFloat()
            }

            shiftRotation = Rotation(yawShift, pitchShift)
            failChronometer.reset()

            ModuleDebug.debugParameter(this, "Chance", chance)
            ModuleDebug.debugParameter(this, "Duration", currentTransitionInDuration)
            ModuleDebug.debugParameter(this, "Shift", shiftRotation)
        }
    }

    /**
     * Generates a complete non-sense rotation.
     */
    fun shiftRotation(rotation: Rotation): Rotation {
        val prevRotation = RotationManager.previousRotation ?: return rotation
        val serverRotation = RotationManager.serverRotation
        val delta = prevRotation - serverRotation
        val nonSenseRotation = rotation + (delta * failFactor) + shiftRotation

        return nonSenseRotation
    }

}
