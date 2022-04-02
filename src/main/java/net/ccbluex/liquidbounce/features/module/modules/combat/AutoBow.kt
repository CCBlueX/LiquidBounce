/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "AutoBow", description = "Automatically shoots an arrow whenever your bow is fully loaded.", category = ModuleCategory.COMBAT)
class AutoBow : Module() {

    private val waitForBowAimbot = BoolValue("WaitForBowAimbot", true)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val bowAimbot = LiquidBounce.moduleManager[BowAimbot::class.java] as BowAimbot

        val thePlayer = mc.thePlayer!!

        if (thePlayer.isUsingItem && classProvider.isItemBow(thePlayer.heldItem?.item) &&
                thePlayer.itemInUseDuration > 20 && (!waitForBowAimbot.get() || !bowAimbot.state || bowAimbot.hasTarget())) {
            thePlayer.stopUsingItem()
            mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, classProvider.getEnumFacing(EnumFacingType.DOWN)))
        }
    }
}
