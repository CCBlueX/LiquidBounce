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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid.canSaveYourself
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid.inputToSavePlayer
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid.unsafeTime
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.getBoundingBoxAt
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull

private typealias Position = Vec3
private typealias Velocity = Vec3

object AntiVoidBlinkMode : AntiVoidMode("Blink") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiVoid.mode

    // Cases in which the AntiVoid protection should not be active.
    override val isExempt
        get() = super.isExempt || ModuleScaffold.running

    private val ignoreDuringCombat by boolean("IgnoreDuringCombat", true)
    private var safePosition: Pair<Position, Velocity>? = null
    private var wasRescued = false

    private val shouldStopBecauseOfCombat
        get() = ignoreDuringCombat && CombatManager.isInCombat


    override fun disable() {
        safePosition = null
        wasRescued = false
        super.disable()
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (mc.screen != null || isExempt) {
            flush(false)
        }
        if (event.origin != TransferOrigin.OUTGOING || player.isDeadOrDying || player.isInWater) {
            return@handler
        }


        when (val packet = event.packet) {
            is ClientboundPlayerPositionPacket,
            is ServerboundResourcePackPacket -> {
                flush(false)
                return@handler
            }

            // Flush on knockback
            is ClientboundSetEntityMotionPacket -> {
                if (packet.id == player.id
                    && (packet.movement.x != 0.0 || packet.movement.y != 0.0 || packet.movement.z != 0.0)
                ) {
                    flush(false)
                    return@handler
                }
            }

            // Flush on explosion
            is ClientboundExplodePacket -> {
                packet.playerKnockback.getOrNull()?.let { knockback ->
                    if (knockback.x != 0.0 || knockback.y != 0.0 || knockback.z != 0.0) {
                        flush(false)
                        return@handler
                    }
                }
            }

            // Flush on damage
            is ClientboundSetHealthPacket -> {
                flush(false)
                return@handler
            }
        }

        // We don't want to lag when we are using an item that is not a food, milk bucket or potion.
        if (player.isUsingItem && player.useItem.isConsumable) {
            flush()
            return@handler
        }

        if (shouldStopBecauseOfCombat || safePosition == null) {
            return@handler
        }

        // we need to queue them, cause tickHandler updates canSaveYourself only every tick
        // but if we send movement packet before tickHandler is invoked, we can't revert it
        // TODO: this has downside that it isn't compatible with other queue based modules (backtrack, fakelag, etc.)
        // possibly we can move tickHandler check to packet and only check on movement packet
        event.action = PacketQueueManager.Action.QUEUE
    }

    @Suppress("unused")
    private val voidHandler = tickHandler(priority = EventPriorityConvention.READ_FINAL_STATE) {
        safePosition?.let { safePos ->
            debugGeometry("safePos") {
                ModuleDebug.DebuggedBox(
                    player.getBoundingBoxAt(safePos.first),
                    Color4b.BLACK.alpha(150)
                )
            }
        }

        if (player.onGround()) {
            wasRescued = true
        }
        if (canSaveYourself && !shouldStopBecauseOfCombat) {
            safePosition = player.position() to player.deltaMovement
            flush()
        }
    }

    private fun flush(shouldKeepPosition: Boolean = true) {
        if (!shouldKeepPosition) {
            safePosition = null
        }
        PacketQueueManager.flush { true }
        unsafeTime.reset()
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent>(EventPriorityConvention.SAFETY_FEATURE) { event ->
        if (wasRescued || shouldStopBecauseOfCombat) {
            return@handler
        }
        val simulatedPlayerInputToFollow = inputToSavePlayer() ?: return@handler

        event.directionalInput = simulatedPlayerInputToFollow.directionalInput
        event.sneak = simulatedPlayerInputToFollow.sneaking
        event.jump = simulatedPlayerInputToFollow.jumping
    }

    override fun rescue(): Boolean {
        val safePosition = safePosition ?: return false

        PacketQueueManager.cancel()
        player.setPos(safePosition.first)
        player.deltaMovement = safePosition.second
        wasRescued = false
        return true
    }

}
