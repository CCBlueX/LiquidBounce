/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedCustom
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedLegitHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedSpeedYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.grim.SpeedGrimCollide
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.ncp.SpeedNCP
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.sentinel.SpeedSentinelDamage
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.spartan.SpeedSpartan524
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.spartan.SpeedSpartan524GroundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.verus.SpeedVerusB3882
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan.SpeedVulcan286
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan.SpeedVulcan288
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan.SpeedVulcanGround286
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog.SpeedHypixelBHop
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.inGame

/**
 * Speed module
 *
 * Allows you to move faster.
 */
object ModuleSpeed : Module("Speed", Category.MOVEMENT) {

    init {
        enableLock()
    }

    /**
     * Initialize speeds choices independently
     *
     * This is useful for the `OnlyOnPotionEffect` choice, which has its own set of modes
     */
    private fun initializeSpeeds(configurable: ChoiceConfigurable<*>) = arrayOf(
        SpeedLegitHop(configurable),
        SpeedCustom(configurable),
        SpeedSpeedYPort(configurable),

        SpeedVerusB3882(configurable),

        SpeedHypixelBHop(configurable),

        SpeedSpartan524(configurable),
        SpeedSpartan524GroundTimer(configurable),

        SpeedSentinelDamage(configurable),

        SpeedVulcan286(configurable),
        SpeedVulcan288(configurable),
        SpeedVulcanGround286(configurable),
        SpeedGrimCollide(configurable),

        SpeedNCP(configurable)
    )

    val modes = choices<Choice>("Mode", { it.choices[0] }, this::initializeSpeeds).apply { tagBy(this) }

    private val notDuringScaffold by boolean("NotDuringScaffold", true)
    private val notDuringFly by boolean("NotDuringFly", true)
    private val notWhileSneaking by boolean("NotWhileSneaking", false)
    private object OnlyOnPotionEffect : ToggleableConfigurable(this, "OnlyOnPotionEffect", false) {

        val potionEffects = choices(
            this,
            "PotionEffect",
            SpeedPotionEffectChoice,
            arrayOf(SpeedPotionEffectChoice, SlownessPotionEffectChoice, BothEffectsChoice)
        )

        val modes = choices<Choice>(this, "Mode", { it.choices[0] }, ModuleSpeed::initializeSpeeds)

        override fun handleEvents(): Boolean {
            // We cannot use our parent super.handleEvents() here, because it has been turned false
            // when [OnlyOnPotionEffect] is enabled
            if (!ModuleSpeed.enabled || !enabled || !inGame || !passesRequirements()) {
                return false
            }

            return potionEffects.activeChoice.checkPotionEffects()
        }

    }

    init {
        tree(OnlyOnPotionEffect)
    }

    override fun handleEvents(): Boolean {
        // Early return if the module is not ready to be used - prevents accessing player when it's null below
        // in case it was forgotten to be checked
        if (!super.handleEvents()) {
            return false
        }

        if (!passesRequirements()) {
            return false
        }

        // We do not want to handle events if the OnlyOnPotionEffect is enabled
        if (OnlyOnPotionEffect.enabled && OnlyOnPotionEffect.potionEffects.activeChoice.checkPotionEffects()) {
            return false
        }

        return true
    }

    private fun passesRequirements(): Boolean {
        if (!inGame) {
            return false
        }

        if (notDuringScaffold && ModuleScaffold.enabled || notDuringFly && ModuleFly.enabled) {
            return false
        }

        // Do NOT access player directly, it can be null in this context
        if (notWhileSneaking && mc.player?.isSneaking == true) {
            return false
        }

        return true
    }

    fun shouldDelayJump(): Boolean {
        return !mc.options.jumpKey.isPressed && (SpeedAntiCornerBump.shouldDelayJump()
            || ModuleCriticals.shouldWaitForJump())
    }

    abstract class PotionEffectChoice(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<PotionEffectChoice>
            get() = OnlyOnPotionEffect.potionEffects

        abstract fun checkPotionEffects(): Boolean
    }
}
