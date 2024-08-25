/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.item.BowItem
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
import net.minecraft.util.math.BlockPos
import net.minecraft.util.Direction

object AutoBow : Module("AutoBow", Category.COMBAT, subjective = true, hideModule = false) {

    private val waitForBowAimbot by BoolValue("WaitForBowAimbot", true)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player

        if (thePlayer.isUsingItem && thePlayer.mainHandStack?.item is BowItem &&
                thePlayer.itemInUseDuration > 20 && (!waitForBowAimbot || !BowAimbot.handleEvents() || BowAimbot.hasTarget())) {
            thePlayer.stopUsingItem()
            sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN))
        }
    }
}
