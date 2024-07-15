package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.chat
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
        // Fail rate
        val chance = (0f..100f).random()
        chat("Trigger: ${failRate / 50f}, Chance: $chance")
        if (failRate / 1f > chance) {
            chat("Fail rate triggered")
            currentTransitionInDuration = transitionInDuration.random()
            shiftRotation = if (Random.nextBoolean()) {
                Rotation((15f..30f).random().toFloat(), (-2.5f..2.5f).random().toFloat())
            } else {
                Rotation((-30f..-15f).random().toFloat(), (-2.5f..2.5f).random().toFloat())
            }
            failChronometer.reset()
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
