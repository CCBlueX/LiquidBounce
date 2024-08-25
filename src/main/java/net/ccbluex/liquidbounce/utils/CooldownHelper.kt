/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.minecraft.item.*
import net.minecraft.potion.Potion
import net.minecraft.util.math.MathHelper
import kotlin.math.min

/**
 * Capable of simulating 1.9+ cooldowns for usage on 1.9+ servers while playing with 1.8.9.
 *
 * @author kawaiinekololis
 */
object CooldownHelper {

    private var lastAttackedTicks = 0

    private var genericAttackSpeed = 0.0

    fun updateGenericAttackSpeed(itemStack: ItemStack?) {
        genericAttackSpeed = when (itemStack?.item) {
            is SwordItem -> 1.6
            is ItemAxe -> {
                val axe = itemStack.item as ItemAxe
                when (axe.toolMaterial) {
                    Item.ToolMaterial.IRON -> 0.9
                    Item.ToolMaterial.WOOD, Item.ToolMaterial.STONE -> 0.8
                    else -> 1.0
                }
            }
            is ItemPickaxe -> 1.2
            is ItemSpade -> 1.0
            is ItemHoe -> {
                val hoe = itemStack.item as ItemHoe
                when (hoe.materialName) {
                    "STONE" -> 2.0
                    "IRON" -> 3.0
                    "DIAMOND" -> 4.0
                    else -> 1.0
                }
            }
            else -> 4.0
        }
        
        if (mc.player.isPotionActive(Potion.digSlowdown)) {
            genericAttackSpeed *= 1.0 - min(1.0, 0.1 * (mc.player.getActivePotionEffect(Potion.digSlowdown).amplifier + 1))
        }
        
        if (mc.player.isPotionActive(Potion.digSpeed)) {
            genericAttackSpeed *= 1.0 + 0.1 * (mc.player.getActivePotionEffect(Potion.digSpeed).amplifier + 1)
        } 
    }

    fun getAttackCooldownProgressPerTick() = 1.0 / genericAttackSpeed * 20.0

    fun getAttackCooldownProgress() = MathHelper.clamp_double((lastAttackedTicks + mc.timer.renderPartialTicks) / getAttackCooldownProgressPerTick(), 0.0, 1.0)

    fun resetLastAttackedTicks() {
        lastAttackedTicks = 0
    }

    fun incrementLastAttackedTicks() {
        lastAttackedTicks++
    }

}
