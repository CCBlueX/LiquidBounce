package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow


import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.BowItem

/**
 * @desc Fast charge options (like FastBow) can be used to charge the bow faster.
 * @warning Should only be used on vanilla minecraft. Most anti cheats patch these kinds of exploits
 *
 * TODO: Add version specific options
 */
object AutoBowFastChargeFeature : ToggleableConfigurable(ModuleAutoBow, "FastCharge", false) {

    private val speed by int("Speed", 20, 3..20)

    private val notInTheAir by boolean("NotInTheAir", true)
    private val notDuringMove by boolean("NotDuringMove", false)
    private val notDuringRegeneration by boolean("NotDuringRegeneration", false)

    private val packetType by enumChoice("PacketType", MovePacketType.FULL)

    @Suppress("unused")
    val tickRepeatable = tickHandler {
        val currentItem = player.activeItem

        // Should speed up game ticks when using bow
        if (currentItem?.item is BowItem) {
            if (notInTheAir && !player.isOnGround) {
                return@tickHandler
            }

            if (notDuringMove && player.moving) {
                return@tickHandler
            }

            if (notDuringRegeneration && player.hasStatusEffect(StatusEffects.REGENERATION)) {
                return@tickHandler
            }

            repeat(speed) {
                if (!player.isUsingItem) {
                    return@repeat
                }

                // Speed up ticks (MC 1.8)
                network.sendPacket(packetType.generatePacket())

                // Show visual effect (not required to work - but looks better)
                player.tickActiveItemStack()
            }
        }
    }
}
