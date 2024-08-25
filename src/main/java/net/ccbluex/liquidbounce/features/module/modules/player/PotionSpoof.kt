/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.potion.PotionEffect
import net.minecraft.potion.Potion.*

object PotionSpoof : Module("PotionSpoof", Category.PLAYER, hideModule = false) {

    private val level by object : IntegerValue("PotionLevel", 2, 1..5) {
        override fun onChanged(oldValue: Int, newValue: Int) = onDisable()
    }

    private val speedValue = BoolValue("Speed", false)
    private val moveSlowDownValue = BoolValue("Slowness", false)
    private val hasteValue = BoolValue("Haste", false)
    private val digSlowDownValue = BoolValue("MiningFatigue", false)
    private val blindnessValue = BoolValue("Blindness", false)
    private val strengthValue = BoolValue("Strength", false)
    private val jumpBoostValue = BoolValue("JumpBoost", false)
    private val weaknessValue = BoolValue("Weakness", false)
    private val regenerationValue = BoolValue("Regeneration", false)
    private val witherValue = BoolValue("Wither", false)
    private val resistanceValue = BoolValue("Resistance", false)
    private val fireResistanceValue = BoolValue("FireResistance", false)
    private val absorptionValue = BoolValue("Absorption", false)
    private val healthBoostValue = BoolValue("HealthBoost", false)
    private val poisonValue = BoolValue("Poison", false)
    private val saturationValue = BoolValue("Saturation", false)
    private val waterBreathingValue = BoolValue("WaterBreathing", false)

    private val potionMap = mapOf(
        moveSpeed.id to speedValue,
        moveSlowdown.id to moveSlowDownValue,
        digSpeed.id to hasteValue,
        digSlowdown.id to digSlowDownValue,
        blindness.id to blindnessValue,
        damageBoost.id to strengthValue,
        jump.id to jumpBoostValue,
        weakness.id to weaknessValue,
        regeneration.id to regenerationValue,
        wither.id to witherValue,
        resistance.id to resistanceValue,
        fireResistance.id to fireResistanceValue,
        absorption.id to absorptionValue,
        healthBoost.id to healthBoostValue,
        poison.id to poisonValue,
        saturation.id to saturationValue,
        waterBreathing.id to waterBreathingValue
    )

    override fun onDisable() {
        mc.player ?: return

        mc.player.activePotionEffects
            .filter { it.duration == 0 && potionMap[it.potionID]?.get() == true }
            .forEach { mc.player.removePotionEffect(it.potionID) }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) =
        potionMap.forEach { (potionId, value) ->
            if (value.get())
                mc.player.addPotionEffect(PotionEffect(potionId, 0, level - 1, false, false))
            else if (mc.player.activePotionEffects.any { it.duration == 0 && it.potionID == potionId })
                mc.player.removePotionEffect(potionId)
        }
}