package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

/**
 * Notifies you about staff actions.
 */
object ModuleAntiStaff : Module("AntiStaff", Category.MISC) {

    object VelocityCheck : ToggleableConfigurable(this, "VelocityCheck", true) {

        val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id) {
                if (packet.velocityX == 0 && packet.velocityZ == 0 && packet.velocityY > 0) {
                    // alert the user
                    alertAboutStaff()
                    return@handler
                }
            }
        }

    }

    init {
        tree(VelocityCheck)
        // todo: add username check via LiquidBounce API
    }

    /**
     * Alert the user about staff watching them.
     */
    private fun alertAboutStaff() {
        notification(
            "Staff Detected",
            "Staff are watching you.",
            NotificationEvent.Severity.INFO
        )
    }

}
