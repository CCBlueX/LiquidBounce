package net.ccbluex.liquidbounce.features.module.modules.combat.autobow

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.QuickAccess.network
import net.minecraft.item.BowItem
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * @desc Fast charge options (like FastBow) can be used to charge the bow faster.
 * @warning Should only be used on vanilla minecraft. Most anti cheats patch these kinds of exploits
 *
 * TODO: Add version specific options
 */
object ModuleAutoBowFastCharge : ToggleableConfigurable(ModuleAutoBow, "FastCharge", false) {
    val packets by int("Packets", 20, 3..20)

    val tickRepeatable =
        handler<GameTickEvent> {
            val player = mc.player ?: return@handler

            val currentItem = player.activeItem

            // Should accelerated game ticks when using bow
            if (currentItem?.item is BowItem) {
                repeat(packets) { // Send a movement packet to simulate ticks (has been patched in 1.19)
                    network.sendPacket(
                        PlayerMoveC2SPacket.OnGroundOnly(true),
                    ) // Just show visual effect (not required to work - but looks better)
                    player.tickActiveItemStack()
                }

                // Shoot with bow (auto shoot has to be enabled)
                // TODO: Depend on Auto Shoot
            }
        }
}
