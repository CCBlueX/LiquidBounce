package net.ccbluex.liquidbounce.utils.aiming.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY

/**
 * The short stop mechanism temporarily halts aiming at the target based on a specified rate.
 */
class ShortStop(owner: EventListener? = null)
    : ToggleableConfigurable(owner, "ShortStop", false) {

    private val stopRate by int("Rate", 3, 1..25, "%")
    private var stopDuration by intRange("Duration", 1..2, 1..5,
        "ticks")

    // A tick meter to track the duration for which the current target has been focused on
    private var ticksElapsed = 0

    // The currently set transition duration, randomized within the defined range
    private var currentTransitionInDuration = stopDuration.random()
    val isInStopState: Boolean
        get() = enabled && ticksElapsed < currentTransitionInDuration

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        if (stopRate > (0..100).random()) {
            currentTransitionInDuration = stopDuration.random()
            ticksElapsed = 0
        } else {
            ticksElapsed++
        }
    }

}
