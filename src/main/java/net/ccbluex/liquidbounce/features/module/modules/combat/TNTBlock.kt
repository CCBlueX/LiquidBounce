/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.item.EntityTNTPrimed
import net.minecraft.item.ItemSword

object TNTBlock : Module("TNTBlock", Category.COMBAT, spacedName = "TNT Block", hideModule = false) {
    private val fuse by IntegerValue("Fuse", 10, 0..80)
    private val range by FloatValue("Range", 9F, 1F..20F)
    private val autoSword by BoolValue("AutoSword", true)
    private var blocked = false

    @EventTarget
    fun onMotionUpdate(event: MotionEvent) {
        val thePlayer = mc.player ?: return
        val theWorld = mc.world ?: return

        for (entity in theWorld.loadedEntityList) {
            if (entity is EntityTNTPrimed && mc.player.getDistanceToEntity(entity) <= range) {
                if (entity.fuse <= fuse) {
                    if (autoSword) {
                        var slot = -1
                        var bestDamage = 1f
                        for (i in 0..8) {
                            val itemStack = thePlayer.inventory.getStackInSlot(i)

                            if (itemStack?.item is ItemSword) {
                                val itemDamage = (itemStack.item as ItemSword).damageVsEntity + 4F

                                if (itemDamage > bestDamage) {
                                    bestDamage = itemDamage
                                    slot = i
                                }
                            }
                        }

                        if (slot != -1 && slot != thePlayer.inventory.selectedSlot) {
                            thePlayer.inventory.selectedSlot = slot
                            mc.interactionManager.updateController()
                        }
                    }

                    if (mc.player.mainHandStack?.item is ItemSword) {
                        mc.gameSettings.keyBindUseItem.pressed = true
                        blocked = true
                    }

                    return
                }
            }
        }

        if (blocked && !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
            mc.gameSettings.keyBindUseItem.pressed = false
            blocked = false
        }
    }
}