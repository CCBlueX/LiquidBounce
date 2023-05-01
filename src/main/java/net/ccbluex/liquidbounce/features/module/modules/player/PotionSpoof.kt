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
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

object PotionSpoof : Module() {

    private val speed by BoolValue("Speed", false)
    private val moveSlowDown by BoolValue("Slowness", false)
    private val haste by BoolValue("Haste", false)
    private val digSlowDown by BoolValue("MiningFatigue", false)
    private val blindness by BoolValue("Blindness", false)
    private val strength by BoolValue("Strength", false)
    private val jumpBoost by BoolValue("JumpBoost", false)
    private val weakness by BoolValue("Weakness", false)
    private val regeneration by BoolValue("Regeneration", false)
    private val wither by BoolValue("Wither", false)
    private val resistance by BoolValue("Resistance", false)
    private val fireResistance by BoolValue("FireResistance", false)
    private val absorption by BoolValue("Absorption", false)
    private val healthBoost by BoolValue("HealthBoost", false)
    private val poison by BoolValue("Poison", false)
    private val saturation by BoolValue("Saturation", false)
    private val waterBreathing by BoolValue("WaterBreathing", false)

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
            if (speed) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.moveSpeed.id, 1337, 1))
            }
            if (haste) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.digSpeed.id, 1337, 1))
            }
            if (moveSlowDown) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.moveSlowdown.id, 1337, 1))
            }
            if (blindness) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.blindness.id, 1337, 1))
            }
            if (strength) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.damageBoost.id, 1337, 1))
            }
            if (jumpBoost) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.jump.id, 1337, 1))
            }
            if (weakness) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.weakness.id, 1337, 1))
            }
            if (regeneration) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.regeneration.id, 1337, 1))
            }
            if (fireResistance) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.fireResistance.id, 1337, 1))
            }
            if (wither) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.wither.id, 1337, 1))
            }
            if (resistance) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.resistance.id, 1337, 1))
            }
            if (absorption) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.absorption.id, 1337, 1))
            }
            if (healthBoost) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.healthBoost.id, 1337, 1))
            }
            if (digSlowDown) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.digSlowdown.id, 1337, 1))
            }
            if (poison) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.poison.id, 1337, 1))
            }
            if (saturation) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.saturation.id, 1337, 1))
            }
            if (waterBreathing) {
                mc.thePlayer.addPotionEffect(PotionEffect(Potion.waterBreathing.id, 1337, 1))
            }
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onShutdown(event: ClientShutdownEvent?) {
        onDisable()
    }
}