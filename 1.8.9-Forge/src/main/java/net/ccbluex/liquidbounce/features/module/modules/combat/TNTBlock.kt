package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.item.EntityTNTPrimed
import net.minecraft.item.ItemSword

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "TNTBlock", description = "Automatically blocks with your sword when TNT around you explodes.", category = ModuleCategory.COMBAT)
class TNTBlock : Module() {
    private val fuseValue = IntegerValue("Fuse", 10, 0, 80)
    private val rangeValue = FloatValue("Range", 9f, 1f, 20f)
    private val autoSwordValue = BoolValue("AutoSword", true)
    private var blocked = false

    @EventTarget
    fun onMotionUpdate(event: MotionEvent?) {
        for (entity in mc.theWorld.loadedEntityList) {
            // Check if the entity is TNT, if it is below the fuse threshold and if it's in range
            if (entity is EntityTNTPrimed
                    && entity.fuse <= fuseValue.get()
                    && mc.thePlayer.getDistanceToEntity(entity) <= rangeValue.get()) {
                if (autoSwordValue.get()) {
                    // Find the best sword
                    var slot = -1
                    var bestDamage = 1f

                    for (i in 0..8) {
                        val itemStack = mc.thePlayer.inventory.getStackInSlot(i)

                        if (itemStack != null && itemStack.item is ItemSword) {
                            val itemDamage = (itemStack.item as ItemSword).damageVsEntity + 4f

                            if (itemDamage > bestDamage) {
                                bestDamage = itemDamage
                                slot = i
                            }
                        }
                    }
                    if (slot != -1 && slot != mc.thePlayer.inventory.currentItem) {
                        mc.thePlayer.inventory.currentItem = slot
                        mc.playerController.updateController()
                    }
                }

                if (mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword) {
                    mc.gameSettings.keyBindUseItem.pressed = true
                    blocked = true
                }
                return
            }

        }
        // Unblock
        if (blocked && !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
            mc.gameSettings.keyBindUseItem.pressed = false
            blocked = false
        }
    }
}