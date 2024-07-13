package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.utils.client.Chronometer

/**
 * The Attention modulates the rotation speed based on the time duration
 * a target has been focused on. Initially, the rotation speed is reduced to smoothly
 * transition to a new target and gradually increases to normal speed. This method
 * enhances aiming by providing smooth adjustments, particularly for fast-moving targets,
 * avoiding abrupt or unnatural flicks.
 */
class Attention(owner: Listenable? = null)
    : ToggleableConfigurable(owner, "Attention", false) {

    // Configuration properties
    private val slowStartFactor by float("SlowStartFactor", 0.6f, 0.05f..0.7f)
    private val transitionDuration by intRange("TransitionDuration", 250..500, 0..5000,
        "ms")

    // A chronometer to track the duration for which the current target has been focused on
    private val focusChronometer = Chronometer()

    // The currently set transition duration, randomized within the defined range
    private var currentTransitionDuration = transitionDuration.random()

    /**
     * The rotation factor is multiplied with the rotation speed to initially slow down
     * the rotation when focusing on a new target, gradually increasing to normal speed.
     */
    val rotationFactor: Float
        get() {
            if (!enabled) {
                return 1f
            }

            val elapsed = focusChronometer.elapsed
            return if (elapsed < currentTransitionDuration) {
                slowStartFactor + (1 - slowStartFactor) * (elapsed.toFloat() / currentTransitionDuration.toFloat())
            } else {
                1f
            }
        }

    /**
     * Resets the chronometer and sets a new randomized transition duration when a new target is acquired.
     */
    fun onNewTarget() {
        currentTransitionDuration = transitionDuration.random()
        focusChronometer.reset()
    }
}
