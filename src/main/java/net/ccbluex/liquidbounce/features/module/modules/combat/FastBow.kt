/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.item.ItemBow
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

@ModuleInfo(name = "FastBow", description = "Turns your bow into a machine gun.", category = ModuleCategory.COMBAT)
class FastBow : Module() {

    private val packetsValue = IntegerValue("Packets", 20, 3, 20)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isUsingItem)
            return

        val currentItem = thePlayer.inventory.getCurrentItem()

        if (currentItem != null && currentItem.item is ItemBow) {
            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mc.thePlayer.currentEquippedItem, 0F, 0F, 0F))

            val yaw = if (RotationUtils.targetRotation != null)
                RotationUtils.targetRotation.yaw
            else
                thePlayer.rotationYaw

            val pitch = if (RotationUtils.targetRotation != null)
                RotationUtils.targetRotation.pitch
            else
                thePlayer.rotationPitch

            for (i in 0 until packetsValue.get())
                mc.netHandler.addToSendQueue(C05PacketPlayerLook(yaw, pitch, true))

            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            thePlayer.itemInUseCount = currentItem.maxItemUseDuration - 1
        }
    }
}