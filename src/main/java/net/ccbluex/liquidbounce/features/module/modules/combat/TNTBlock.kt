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
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.option.GameOptions
import net.minecraft.entity.TntEntity
import net.minecraft.item.SwordItem

object TNTBlock : Module("TNTBlock", Category.COMBAT, spacedName = "TNT Block", hideModule = false) {
    private val fuse by IntegerValue("Fuse", 10, 0..80)
    private val range by FloatValue("Range", 9F, 1F..20F)
    private val autoSword by BoolValue("AutoSword", true)
    private var blocked = false

    @EventTarget
    fun onMotionUpdate(event: MotionEvent) {
        val thePlayer = mc.player ?: return
        val theWorld = mc.world ?: return

        for (entity in theWorld.entities) {
            if (entity is TntEntity && mc.player.getDistanceToEntityBox(entity) <= range) {
                if (entity.fuseTimer <= fuse) {
                    if (autoSword) {
                        var slot = -1
                        var bestDamage = 1f
                        for (i in 0..8) {
                            val itemStack = thePlayer.inventory.getInvStack(i)

                            if (itemStack?.item is SwordItem) {
                                val itemDamage = (itemStack.item as SwordItem).attackDamage + 4F

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

                    if (mc.player.mainHandStack?.item is SwordItem) {
                        mc.options.useKey.pressed = true
                        blocked = true
                    }

                    return
                }
            }
        }

        if (blocked && !GameOptions.isPressed(mc.options.useKey)) {
            mc.options.useKey.pressed = false
            blocked = false
        }
    }
}