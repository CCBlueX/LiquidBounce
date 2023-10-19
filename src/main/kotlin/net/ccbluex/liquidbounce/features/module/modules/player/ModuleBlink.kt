/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleBadWifi
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBreadcrumbs.makeLines
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.RigidPlayerSimulation
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.util.math.Vec3d
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Blink module
 *
 * Makes it look as if you were teleporting to other players.
 */

object ModuleBlink : Module("Blink", Category.PLAYER) {
    private object Pulse : ToggleableConfigurable(this, "Pulse", false) {
        val delay by int("Delay", 20, 10..100)
    }

    private val dummy by boolean("Dummy", false)
    private val ambush by boolean("Ambush", false)
    private val evadeArrows by boolean("EvadeArrows", true)

    private object BreadcrumbsOption : ToggleableConfigurable(this, "Breadcrumbs", false) {
        val breadcrumbscolor by color("BreadcrumbsColor", Color4b(255, 179, 72, 255))
        val breadcrumbsrainbow by boolean("BreadcrumbsRainbow", false)
    }

    private object AutoResetOption : ToggleableConfigurable(this, "AutoReset", false) {
        val resetAfter by int("ResetAfter", 100, 1..1000)
        val action by enumChoice("ResetAction", ResetAction.RESET, ResetAction.values())
    }

    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var startPos: Vec3d? = null
    private var fakePlayer: OtherClientPlayerEntity? = null
    private var disablelogger = false
    private val positions = mutableListOf<Double>()

    private var positionPackets = AtomicInteger(0)

    init {
        tree(Pulse)
        tree(BreadcrumbsOption)
        tree(AutoResetOption)
    }

    override fun enable() {
        if (ModuleBadWifi.enabled) {
            ModuleBadWifi.enabled = false

            notification("Compatibility error", "Blink is incompatible with BadWIFI", NotificationEvent.Severity.ERROR)
            return
        }

        if (!Pulse.enabled && dummy) {
            val clone = OtherClientPlayerEntity(world, player.gameProfile)

            clone.headYaw = player.headYaw
            clone.copyPositionAndRotation(player)
            /**
             * A different UUID has to be set, to avoid [fakePlayer] from being invisible to [player]
             * @see net.minecraft.world.entity.EntityIndex.add
             */
            clone.uuid = UUID.randomUUID()
            world.addEntity(clone.id, clone)

            fakePlayer = clone
        }

        startPos = player.pos

        synchronized(positions) {
            positionPackets.set(0)
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        if (!BreadcrumbsOption.enabled)
            return@handler

        val matrixStack = event.matrixStack
        val color = if (BreadcrumbsOption.breadcrumbsrainbow) rainbow() else BreadcrumbsOption.breadcrumbscolor

        synchronized(positions) {

            renderEnvironmentForWorld(matrixStack) {
                withColor(color) {
                    drawLineStrip(*makeLines(color, positions, event.partialTicks))
                }
            }
        }
    }

    override fun disable() {
        if (mc.player == null) {
            return
        }

        blink()
        removeClone()
    }

    private fun removeClone() {
        val clone = fakePlayer ?: return

        world.removeEntity(clone.id, Entity.RemovalReason.DISCARDED)
        fakePlayer = null
    }

    val packetHandler = handler<PacketEvent>(priority = -1) { event ->
        if (mc.player == null || disablelogger || event.origin != TransferOrigin.SEND) {
            return@handler
        }

        val packet = event.packet

        if (ambush && packet is PlayerInteractEntityC2SPacket) {
            enabled = false

            return@handler
        }

        if (packet is PlayerMoveC2SPacket || packet is PlayerInteractBlockC2SPacket || packet is HandSwingC2SPacket || packet is PlayerActionC2SPacket || packet is PlayerInteractEntityC2SPacket) {
            if (packet is PlayerMoveC2SPacket && !packet.changePosition) {
                return@handler
            }

            if (packet is PlayerMoveC2SPacket) {
                synchronized(positions) {
                    positions.addAll(listOf(packet.x, packet.y, packet.z))

                    positionPackets.getAndIncrement()
                }
            }

            event.cancelEvent()

            packets.add(packet)
        }
    }

    val repeatable = repeatable {
        if (Pulse.enabled) {
            wait(Pulse.delay)
            blink()
        } else if (evadeArrows) {
            val firstPositionPacket = (packets.firstOrNull { it is PlayerMoveC2SPacket && it.changePosition } ?: return@repeatable) as PlayerMoveC2SPacket

            if (getInflictedHit(Vec3d(firstPositionPacket.x, firstPositionPacket.y, firstPositionPacket.z)) == null) {
                return@repeatable
            }

            val evadingPacket = findPacketThatAvoidsArrows()

            // We have found no packet that avoids getting hit? Then we default to blinking.
            // AutoDoge might save the situation...
            if (evadingPacket == null) {
                notification("Blink", "Unable to evade arrow. Blinking.", NotificationEvent.Severity.INFO)

                blink()

                enabled = false
            } else if (evadingPacket.ticksToImpact != null) {
                notification("Blink", "Trying to evade arrow...", NotificationEvent.Severity.INFO)

                blinkNPackets(evadingPacket.idx + 1)
            } else {
                notification("Blink", "Arrow evaded.", NotificationEvent.Severity.INFO)

                blinkNPackets(evadingPacket.idx + 1)
            }
        }
    }

    data class EvadingPacket(
        val idx: Int,
        /**
         * Ticks until impact. Null if evaded
         */
        val ticksToImpact: Int?
    )

    /**
     * Returns the index of the first position packet that avoids all arrows in the next X seconds
     */
    private fun findPacketThatAvoidsArrows(): EvadingPacket? {
        var packetIndex = 0

        var lastPosition: Vec3d? = null

        var bestPacketPosition: Vec3d? = null
        var bestPacketIdx: Int? = null
        var bestTimeToImpact = 0

        for (packet in this.packets) {
            packetIndex += 1

            if (packet !is PlayerMoveC2SPacket)
                continue
            if (!packet.changePosition)
                continue

            val packetPosition = Vec3d(packet.x, packet.y, packet.z)

            // Process packets only if they are at least some distance away from each other
            if (lastPosition != null) {
                if (lastPosition.squaredDistanceTo(packetPosition) < 0.9 * 0.9) {
                    continue
                }
            }

            lastPosition = packetPosition

            val inflictedHit = getInflictedHit(packetPosition)

            if (inflictedHit == null)
                return EvadingPacket(packetIndex - 1, null)
            else if (inflictedHit.tickDelta > bestTimeToImpact) {
                bestTimeToImpact = inflictedHit.tickDelta
                bestPacketIdx = packetIndex - 1
                bestPacketPosition = packetPosition
            }
        }

        // If the evading packet is less than one player hitbox away from the current position, we should rather
        // call the evasion a failure
        if (bestPacketIdx != null && bestPacketPosition!!.squaredDistanceTo(lastPosition!!) > 0.9) {
            return EvadingPacket(bestPacketIdx, bestTimeToImpact)
        }

        return null
    }

    private fun getInflictedHit(pos: Vec3d): ModuleAutoDodge.HitInfo? {
        val arrows = ModuleAutoDodge.findFlyingArrows(world)

        val playerSimulation = RigidPlayerSimulation(pos)

        return ModuleAutoDodge.getInflictedHits(playerSimulation, arrows, maxTicks = 40) {}
    }

    val playerMoveHandler = handler<PlayerMovementTickEvent> {
        if (AutoResetOption.enabled && positionPackets.get() > AutoResetOption.resetAfter) {
            when (AutoResetOption.action) {
                ResetAction.RESET -> reset()
                ResetAction.BLINK -> blink()
            }

            notification("Blink", "Auto reset", NotificationEvent.Severity.INFO)

            enabled = false
        }
    }

    private fun blink() {
        try {
            runWithDisabledLogger {
                while (!packets.isEmpty()) {
                    network.sendPacket(packets.take())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            synchronized(positions) { positions.clear() }

            positionPackets.set(0)
        }
    }

    /**
     * Blinks [n] packets, removes corresponding positions
     */
    private fun blinkNPackets(n: Int) {
        var nPositionPackets = 0

//        println("Sending $n/${packets.size} packets.")

        try {
            runWithDisabledLogger {
                for (i in 0..n) {
                    if (packets.isEmpty())
                        break

                    val packet = packets.take()

                    if (isPositionPacket(packet))
                        nPositionPackets += 1

                    network.sendPacket(packet)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            synchronized(positions) {
                // Remove first nPositionPackets elements from list
                positions.subList(0, nPositionPackets * 3).clear()

                positionPackets.set(positions.size / 3)
            }


            val firstPosPacket = packets.find { it is PlayerMoveC2SPacket && it.changePosition } as PlayerMoveC2SPacket?

            startPos = if (firstPosPacket == null) {
                player.pos
            } else {
                Vec3d(firstPosPacket.x, firstPosPacket.y, firstPosPacket.z)
            }
        }

    }

    private fun isPositionPacket(packet: Packet<*>?) = packet is PlayerMoveC2SPacket && packet.changePosition

    private fun reset() {
        packets.clear()
        synchronized(positions) {
            positions.clear()
            positionPackets.set(0)
        }

        val start = startPos ?: return

        player.setPosition(start)

        player.prevX = start.x
        player.prevY = start.y
        player.prevZ = start.z
        player.lastRenderX = start.x
        player.lastRenderY = start.y
        player.lastRenderZ = start.z

        player.velocity.times(0.0)

        player.updatePositionAndAngles(start.x, start.y, start.z, player.yaw, player.pitch)
    }

    private inline fun runWithDisabledLogger(fn: () -> Unit) {
        disablelogger = true

        try {
            fn()
        } finally {
            disablelogger = false
        }
    }

    enum class ResetAction(override val choiceName: String) : NamedChoice {
        RESET("Reset"),
        BLINK("Blink");
    }
}
