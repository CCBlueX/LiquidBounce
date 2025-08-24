/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModeBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModeFirework
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModePitch40Infinite
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModeStatic
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModeVanilla
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.set
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

/**
 * ElytraFly module
 *
 * Makes elytra flying easier to control.
 */
object ModuleElytraFly : ClientModule("ElytraFly", Category.MOVEMENT) {

    private val instant by multiEnumChoice("Instant", Instant.STOP)

    object Speed : ToggleableConfigurable(this, "Speed", true) {
        val vertical by float("Vertical", 0.5f, 0.0f..5f)
        val horizontal by float("Horizontal", 1f, 0.0f..8f)
    }



    private val notInFluid by boolean("NotInFluid", false)

    /**
     * Spams elytra starting so that we switch between falling and gliding all the time and so don't use any elytra
     * durability.
     */
    private val durabilityExploit by boolean("DurabilityExploit", false)

    init {
        tree(Speed)
    }

    internal val modes = choices("Mode", ElytraFlyModeStatic, arrayOf(
        ElytraFlyModeStatic,
        ElytraFlyModeVanilla,
        ElytraFlyModeBoost,
        ElytraFlyModeFirework,
        ElytraFlyModePitch40Infinite
    ))

    private var needsToRestart = false

    override fun onEnabled() {
        needsToRestart = false
    }

    override fun onDisabled() {
        needsToRestart = true
    }

    // checks and start logic
    @Suppress("unused", "ComplexCondition")
    private val tickHandler = tickHandler {
        if (shouldNotOperate()) {
            needsToRestart = false
            return@tickHandler
        }

        val stop =
            mc.options.sneakKey.isPressed
            && Instant.STOP in instant
            && player.isOnGround
            || notInFluid && player.isInFluid

        if (stop && player.isGliding) {
            player.stopGliding()
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING))
            needsToRestart = false
            return@tickHandler
        }

        if (player.isGliding) {
            // we're already flying, yay
            val activeChoice = modes.activeChoice
            if (Speed.enabled) {
                activeChoice.onTick()
            }

            val modeDoesNotPreventStopping = activeChoice !is ElytraFlyModeStatic ||
                !activeChoice.durabilityExploitNotWhileMove || !player.moving
            if (durabilityExploit && modeDoesNotPreventStopping) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING))
                needsToRestart = true
            }
        } else if (
            player.input.playerInput.jump
            && player.velocity.y != 0.0
            && Instant.START in instant
            || needsToRestart
        ) {
            // If the player has an elytra and wants to fly instead

            // Jump must be off due to abnormal speed boosts
            player.input.set(jump = false)
            player.startGliding()
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING))
        }
    }

    fun shouldNotOperate(): Boolean {
        if (player.vehicle != null) {
            return true
        }

        if (player.abilities.creativeMode || player.hasStatusEffect(StatusEffects.LEVITATION)) {
            return true
        }

        // Find the chest slot
        val chestSlot = player.getEquippedStack(EquipmentSlot.CHEST)

        // If the player doesn't have an elytra in the chest slot or is in fluids
        return chestSlot.item != Items.ELYTRA || chestSlot.willBreakNextUse()
    }

    private enum class Instant(
        override val choiceName: String
    ) : NamedChoice {
        START("Start"),
        STOP("Stop")
    }
}
