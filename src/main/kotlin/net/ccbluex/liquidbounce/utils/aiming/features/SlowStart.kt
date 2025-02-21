package net.ccbluex.liquidbounce.utils.aiming.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY

/**
 * The up ramp modulates the rotation speed based on the time duration
 * a target has been focused on. Initially, the rotation speed is reduced to smoothly
 * transition to a new target and gradually increases to normal speed. This method
 * enhances aiming by providing smooth adjustments, particularly for fast-moving targets,
 * avoiding abrupt or unnatural flicks.
 */
class SlowStart(owner: EventListener? = null)
    : ToggleableConfigurable(owner, "SlowStart", false) {

    // Configuration properties
    private val slowStartFactor by float("SlowStartFactor", 0.6f, 0.01f..0.99f)
    private val transitionDuration by intRange("TransitionDuration", 1..3, 1..20,
        "ticks")

    // Triggers
    val onEnemyChange by boolean("OnEnemyChange", true)
    val onZeroRotationDifference by boolean("OnZeroRotationDifference", false)

    // A tick meter to track the duration for which the current target has been focused on
    private var ticksElapsed = 0

    // The currently set transition duration, randomized within the defined range
    private var currentTransitionDuration = transitionDuration.random()

    /**
     * The rotation factor is multiplied with the rotation speed to initially slow down
     * the rotation when focusing on a new target, gradually increasing to normal speed.
     */
    val rotationFactor: Float
        get() {
            if (!running) {
                return 1f
            }

            val elapsed = ticksElapsed
            return if (elapsed <= currentTransitionDuration) {
                slowStartFactor + (1 - slowStartFactor) * (elapsed.toFloat() / currentTransitionDuration.toFloat())
            } else {
                1f
            }
        }

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        ticksElapsed++
    }

    /**
     * Resets the chronometer and sets a new randomized transition duration when a trigger event occurs.
     */
    fun onTrigger() {
        currentTransitionDuration = transitionDuration.random()
        ticksElapsed = 0
    }

}
