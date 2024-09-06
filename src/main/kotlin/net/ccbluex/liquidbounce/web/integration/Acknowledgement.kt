package net.ccbluex.liquidbounce.web.integration

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.VirtualScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.Chronometer

/**
 * Acknowledgement is used to detect desyncs between the integration browser and the client.
 * It is reset when the client opens a new screen and confirmed when the integration browser
 * opens the same screen.
 *
 * If the acknowledgement is not confirmed after 500ms, the integration browser will be reloaded.
 */
object Acknowledgement : Listenable {

    val since: Chronometer = Chronometer()
    var confirmed: Boolean = false

    @Suppress("unused")
    val isDesynced
        get() = !confirmed && since.hasElapsed(1000)

    fun confirm() {
        confirmed = true
    }

    fun reset() {
        since.reset()
        confirmed = false
    }

    @Suppress("unused")
    private val handleVirtualChange = handler<VirtualScreenEvent> {
        reset()
    }

}
