/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.item.*
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
            is AxeItem -> {
                val axe = itemStack.item as AxeItem
                when (axe.material) {
                    Item.ToolMaterialType.IRON -> 0.9
                    Item.ToolMaterialType.WOOD, Item.ToolMaterialType.STONE -> 0.8
                    else -> 1.0
                }
            }
            is PickaxeItem -> 1.2
            is ShovelItem -> 1.0
            is HoeItem -> {
                val hoe = itemStack.item as HoeItem
                when (hoe.itemGroup.id) {
                    "STONE" -> 2.0
                    "IRON" -> 3.0
                    "DIAMOND" -> 4.0
                    else -> 1.0
                }
            }
            else -> 4.0
        }
        
        if (mc.player.hasStatusEffect(StatusEffect.MINING_FATIGUE)) {
            genericAttackSpeed *= 1.0 - min(1.0, 0.1 * (mc.player.getEffectInstance(StatusEffect.MINING_FATIGUE).amplifier + 1))
        }
        
        if (mc.player.hasStatusEffect(StatusEffect.HASTE)) {
            genericAttackSpeed *= 1.0 + 0.1 * (mc.player.getEffectInstance(StatusEffect.HASTE).amplifier + 1)
        } 
    }

    fun getAttackCooldownProgressPerTick() = 1.0 / genericAttackSpeed * 20.0

    fun getAttackCooldownProgress() = MathHelper.clamp((lastAttackedTicks + mc.ticker.lastFrameDuration) / getAttackCooldownProgressPerTick(), 0.0, 1.0)

    fun resetLastAttackedTicks() {
        lastAttackedTicks = 0
    }

    fun incrementLastAttackedTicks() {
        lastAttackedTicks++
    }

}
