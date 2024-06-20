package net.ccbluex.liquidbounce.features.module.modules.movement.speed

import net.minecraft.entity.effect.StatusEffects

object SpeedPotionEffectChoice : ModuleSpeed.PotionEffectChoice("Speed") {
    private val levelRange by intRange("LevelRange", 1..2, 0..4)

    override fun checkPotionEffects(): Boolean {
        val level = mc.player?.getStatusEffect(StatusEffects.SPEED)?.amplifier?.plus(1) ?: 0
        return level in levelRange
    }
}

object SlownessPotionEffectChoice : ModuleSpeed.PotionEffectChoice("Slowness") {
    private val levelRange by intRange("LevelRange", 1..2, 0..4)

    override fun checkPotionEffects(): Boolean {
        val level = mc.player?.getStatusEffect(StatusEffects.SLOWNESS)?.amplifier?.plus(1) ?: 0
        return level in levelRange
    }
}

object BothEffectsChoice : ModuleSpeed.PotionEffectChoice("Both") {
    private val boostRange by floatRange("PotionEffectsBoostRange", 1.15f..1.35f, 0.25f..2f)

    /**
     * Calculates a resulting movement multiplier depending on the potion effects applied to the player
     *
     * For instance, the player will move 1.36 times faster
     * if it has the following potion effects: Speed III and Slowness I
     */
    private fun getMultiplier(speedLevel: Int, slownessLevel: Int): Float {
        val speedMultiplier = 1f + (0.2f * speedLevel)
        val slownessMultiplier = 1f - (0.15f * slownessLevel)
        return speedMultiplier * slownessMultiplier.coerceAtLeast(0f)
    }

    override fun checkPotionEffects(): Boolean {
        val speedLevel = mc.player?.getStatusEffect(StatusEffects.SPEED)?.amplifier?.plus(1) ?: 0
        val slownessLevel = mc.player?.getStatusEffect(StatusEffects.SLOWNESS)?.amplifier?.plus(1) ?: 0

        return getMultiplier(speedLevel, slownessLevel) in boostRange
    }
}
