package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.prepareAttackEnvironment
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.item.InventoryTracker
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult

internal object FailSwing : ToggleableConfigurable(ModuleKillAura, "FailSwing", false) {

    /**
     * Additional range for fail swing to work
     */
    val additionalRange by float("AdditionalRange", 2f, 0f..10f)

    suspend fun Sequence<*>.dealWithFakeSwing(target: Entity?) {
        if (!enabled) {
            return
        }

        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        if (isInInventoryScreen && !ModuleKillAura.ignoreOpenInventory && !ModuleKillAura.simulateInventoryClosing) {
            return
        }

        val raycastType = mc.crosshairTarget?.type

        val range = ModuleKillAura.range + additionalRange
        val entity = target ?: world.findEnemy(0f..range) ?: return

        if (entity.isRemoved || entity.boxedDistanceTo(player) > range || raycastType != HitResult.Type.MISS) {
            return
        }

        if (ModuleKillAura.clickScheduler.goingToClick) {
            prepareAttackEnvironment {
                ModuleKillAura.clickScheduler.clicks {
                    if (ModuleKillAura.swing) {
                        player.swingHand(Hand.MAIN_HAND)
                    } else {
                        network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                    }
                    true
                }
            }
        }
    }

}
