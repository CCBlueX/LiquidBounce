/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModeBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModeFirework
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModePitch40Infinite
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModeStatic
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModeVanilla
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.set
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items

/**
 * ElytraFly module
 *
 * Makes elytra flying easier to control.
 */
object ModuleElytraFly : ClientModule("ElytraFly", ModuleCategories.MOVEMENT) {

    private val instant by multiEnumChoice("Instant", Instant.STOP)

    object Speed : ToggleableValueGroup(this, "Speed", true) {
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
            mc.options.keyShift.isDown
            && Instant.STOP in instant
            && player.onGround()
            || notInFluid && player.isInLiquid

        if (stop && player.isFallFlying) {
            player.stopFallFlying()
            network.send(
                ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING)
            )
            needsToRestart = false
            return@tickHandler
        }

        if (player.isFallFlying) {
            // we're already flying, yay
            val activeChoice = modes.activeMode
            if (Speed.enabled) {
                activeChoice.onTick()
            }

            val modeDoesNotPreventStopping = activeChoice !is ElytraFlyModeStatic ||
                !activeChoice.durabilityExploitNotWhileMove || !player.moving
            if (durabilityExploit && modeDoesNotPreventStopping) {
                network.send(
                    ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING)
                )
                needsToRestart = true
            }
        } else if (
            player.input.keyPresses.jump
            && player.deltaMovement.y != 0.0
            && Instant.START in instant
            || needsToRestart
        ) {
            // If the player has an elytra and wants to fly instead

            // Jump must be off due to abnormal speed boosts
            player.input.set(jump = false)
            player.startFallFlying()
            network.send(
                ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING)
            )
        }
    }

    fun shouldNotOperate(): Boolean {
        if (player.vehicle != null) {
            return true
        }

        if (player.abilities.instabuild || player.hasEffect(MobEffects.LEVITATION)) {
            return true
        }

        // Find the chest slot
        val chestSlot = player.getItemBySlot(EquipmentSlot.CHEST)

        // If the player doesn't have an elytra in the chest slot or is in fluids
        return chestSlot.item != Items.ELYTRA || chestSlot.nextDamageWillBreak()
    }

    private enum class Instant(
        override val tag: String
    ) : Tagged {
        START("Start"),
        STOP("Stop")
    }
}
