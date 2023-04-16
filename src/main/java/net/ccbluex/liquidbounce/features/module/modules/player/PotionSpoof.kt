<<<<<<< HEAD
/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

object PotionSpoof : Module("PotionSpoof", ModuleCategory.PLAYER) {

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

    override fun onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.removePotionEffectClient(Potion.moveSpeed.id)
            mc.thePlayer.removePotionEffectClient(Potion.digSpeed.id)
            mc.thePlayer.removePotionEffectClient(Potion.moveSlowdown.id)
            mc.thePlayer.removePotionEffectClient(Potion.blindness.id)
            mc.thePlayer.removePotionEffectClient(Potion.damageBoost.id)
            mc.thePlayer.removePotionEffectClient(Potion.jump.id)
            mc.thePlayer.removePotionEffectClient(Potion.weakness.id)
            mc.thePlayer.removePotionEffectClient(Potion.regeneration.id)
            mc.thePlayer.removePotionEffectClient(Potion.fireResistance.id)
            mc.thePlayer.removePotionEffectClient(Potion.wither.id)
            mc.thePlayer.removePotionEffectClient(Potion.resistance.id)
            mc.thePlayer.removePotionEffectClient(Potion.absorption.id)
            mc.thePlayer.removePotionEffectClient(Potion.healthBoost.id)
            mc.thePlayer.removePotionEffectClient(Potion.digSlowdown.id)
            mc.thePlayer.removePotionEffectClient(Potion.poison.id)
            mc.thePlayer.removePotionEffectClient(Potion.saturation.id)
            mc.thePlayer.removePotionEffectClient(Potion.waterBreathing.id)
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: UpdateEvent?) {
        if(state) {
            if (speedValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.moveSpeed.id, 1337, 1))
            }
            if (hasteValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.digSpeed.id, 1337, 1))
            }
            if (moveSlowDownValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.moveSlowdown.id, 1337, 1))
            }
            if (blindnessValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.blindness.id, 1337, 1))
            }
            if (strengthValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.damageBoost.id, 1337, 1))
            }
            if (jumpBoostValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.jump.id, 1337, 1))
            }
            if (weaknessValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.weakness.id, 1337, 1))
            }
            if (regenerationValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.regeneration.id, 1337, 1))
            }
            if (fireResistanceValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.fireResistance.id, 1337, 1))
            }
            if (witherValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.wither.id, 1337, 1))
            }
            if (resistanceValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.resistance.id, 1337, 1))
            }
            if (absorptionValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.absorption.id, 1337, 1))
            }
            if (healthBoostValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.healthBoost.id, 1337, 1))
            }
            if (digSlowDownValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.digSlowdown.id, 1337, 1))
            }
            if (poisonValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.poison.id, 1337, 1))
            }
            if (saturationValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.saturation.id, 1337, 1))
            }
            if (waterBreathingValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.waterBreathing.id, 1337, 1))
            }
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onShutdown(event: ClientShutdownEvent?) {
        onDisable()
    }
=======
/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

object PotionSpoof : Module("PotionSpoof", ModuleCategory.PLAYER) {

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

    override fun onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.removePotionEffectClient(Potion.moveSpeed.id)
            mc.thePlayer.removePotionEffectClient(Potion.digSpeed.id)
            mc.thePlayer.removePotionEffectClient(Potion.moveSlowdown.id)
            mc.thePlayer.removePotionEffectClient(Potion.blindness.id)
            mc.thePlayer.removePotionEffectClient(Potion.damageBoost.id)
            mc.thePlayer.removePotionEffectClient(Potion.jump.id)
            mc.thePlayer.removePotionEffectClient(Potion.weakness.id)
            mc.thePlayer.removePotionEffectClient(Potion.regeneration.id)
            mc.thePlayer.removePotionEffectClient(Potion.fireResistance.id)
            mc.thePlayer.removePotionEffectClient(Potion.wither.id)
            mc.thePlayer.removePotionEffectClient(Potion.resistance.id)
            mc.thePlayer.removePotionEffectClient(Potion.absorption.id)
            mc.thePlayer.removePotionEffectClient(Potion.healthBoost.id)
            mc.thePlayer.removePotionEffectClient(Potion.digSlowdown.id)
            mc.thePlayer.removePotionEffectClient(Potion.poison.id)
            mc.thePlayer.removePotionEffectClient(Potion.saturation.id)
            mc.thePlayer.removePotionEffectClient(Potion.waterBreathing.id)
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: UpdateEvent?) {
        if(state) {
            if (speedValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.moveSpeed.id, 1337, 1))
            }
            if (hasteValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.digSpeed.id, 1337, 1))
            }
            if (moveSlowDownValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.moveSlowdown.id, 1337, 1))
            }
            if (blindnessValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.blindness.id, 1337, 1))
            }
            if (strengthValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.damageBoost.id, 1337, 1))
            }
            if (jumpBoostValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.jump.id, 1337, 1))
            }
            if (weaknessValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.weakness.id, 1337, 1))
            }
            if (regenerationValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.regeneration.id, 1337, 1))
            }
            if (fireResistanceValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.fireResistance.id, 1337, 1))
            }
            if (witherValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.wither.id, 1337, 1))
            }
            if (resistanceValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.resistance.id, 1337, 1))
            }
            if (absorptionValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.absorption.id, 1337, 1))
            }
            if (healthBoostValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.healthBoost.id, 1337, 1))
            }
            if (digSlowDownValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.digSlowdown.id, 1337, 1))
            }
            if (poisonValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.poison.id, 1337, 1))
            }
            if (saturationValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.saturation.id, 1337, 1))
            }
            if (waterBreathingValue.get()) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.waterBreathing.id, 1337, 1))
            }
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onShutdown(event: ClientShutdownEvent?) {
        onDisable()
    }
>>>>>>> origin/legacy
}