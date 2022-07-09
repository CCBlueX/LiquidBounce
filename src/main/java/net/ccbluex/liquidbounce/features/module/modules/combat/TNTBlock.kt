/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.getEntitiesInRadius
import net.ccbluex.liquidbounce.utils.extensions.isEnemy
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "TNTBlock", description = "Automatically blocks with your sword when TNT around you explodes.", category = ModuleCategory.COMBAT)
class TNTBlock : Module()
{
    private val fuseValue = IntegerValue("Fuse", 10, 0, 80)
    private val rangeValue = FloatValue("Range", 9F, 1F, 20F)
    private val autoSwordValue = BoolValue("AutoSword", true)

    private var blocked = false

    @EventTarget
    fun onMotionUpdate(@Suppress("UNUSED_PARAMETER") event: MotionEvent)
    {
        val thePlayer = mc.thePlayer ?: return
        val theWorld = mc.theWorld ?: return
        val gameSettings = mc.gameSettings

        val range = rangeValue.get()
        val fuse = fuseValue.get()

        val provider = classProvider
        if (theWorld.getEntitiesInRadius(thePlayer, range + 2.0).any { it is EntityTNTPrimed && thePlayer.getDistanceToEntity(it) <= range && it.asEntityTNTPrimed().fuse <= fuse || it is EntityCreeper && thePlayer.getDistanceToEntity(it) <= range && it.asEntityLivingBase()false is Enemy && it.asEntityCreeper().creeperState })
        {
            if (autoSwordValue.get())
            {
                val inventory = thePlayer.inventory
                val slot = (0..8).mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { it.second.item is ItemSword }.maxBy { (it.second.item? as ItemSword?.damageVsEntity ?: 0f) + 4f }?.first ?: -1

                if (slot != -1 && slot != inventory.currentItem)
                {
                    inventory.currentItem = slot
                    mc.playerController.updateController()
                }
            }

            val heldItem = thePlayer.heldItem

            if (heldItem?.item is ItemSword)
            {
                gameSettings.keyBindUseItem.pressed = true
                blocked = true
            }

            return
        }

        if (blocked && gameSettings.keyBindUseItem !is KeyDown)
        {
            gameSettings.keyBindUseItem.pressed = false
            blocked = false
        }
    }

    override val tag: String
        get() = "${fuseValue.get()}"
}
