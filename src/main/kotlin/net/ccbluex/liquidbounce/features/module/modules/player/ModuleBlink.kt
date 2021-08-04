/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleBadWifi
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBreadcrumbs
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.RenderEngine
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.util.math.Vec3d
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Blink module
 *
 * Makes it look as if you were teleporting to other players.
 */

object ModuleBlink : Module("Blink", Category.PLAYER) {
    private val pulse by boolean("Pulse", false)
    private val dummy by boolean("Dummy", false)
    private val ambush by boolean("Ambush", false)
    private val delay by int("Delay", 20, 10..100)

    private object BreadcrumbsOption : ToggleableConfigurable(this, "Breadcrumbs", false) {
        val breadcrumbscolor by color("BreadcrumbsColor", Color4b(255, 179, 72, 255))
        val breadcrumbsrainbow by boolean("BreadcrumbsRainbow", false)
    }

    private object AutoResetOption : ToggleableConfigurable(this, "AutoReset", false) {
        val resetAfter by int("ResetAfter", 100, 1..1000)
    }

    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var startPos: Vec3d? = null
    private var fakeplayer: OtherClientPlayerEntity? = null
    private var disablelogger = false
    private val positions = mutableListOf<Double>()

    private var positionPackets = AtomicInteger(0)

    init {
        tree(BreadcrumbsOption)
        tree(AutoResetOption)
    }

    override fun enable() {
        if (ModuleBadWifi.enabled) {
            this.enabled = false

            notification("Compatibility error", "Blink is incompatible with BadWIFI", NotificationEvent.Severity.ERROR)
        }

        if (!pulse && dummy) {
            val faker = OtherClientPlayerEntity(world, player.gameProfile)

            faker.headYaw = player.headYaw
            faker.copyPositionAndRotation(player)
            world.addEntity(faker.id, faker)

            fakeplayer = faker
        }

        startPos = player.pos

        positionPackets.set(0)
    }

    val renderHandler = handler<EngineRenderEvent> {
        val color = if (BreadcrumbsOption.breadcrumbsrainbow) rainbow() else BreadcrumbsOption.breadcrumbscolor

        synchronized(positions) {
            if (BreadcrumbsOption.enabled) {
                RenderEngine.enqueueForRendering(
                    RenderEngine.CAMERA_VIEW_LAYER,
                    ModuleBreadcrumbs.createBreadcrumbsRenderTask(color, this.positions, it.tickDelta)
                )
            }
        }
    }

    override fun disable() {
        if (mc.player == null) {
            return
        }

        chat(regular(positionPackets.toString()))

        blink()

        removeFakePlayer()
    }

    private fun removeFakePlayer() {
        val faker = this.fakeplayer

        if (faker != null) {
            world.removeEntity(faker.id, Entity.RemovalReason.UNLOADED_TO_CHUNK)

            this.fakeplayer = null
        }
    }

    val packetHandler = handler<PacketEvent>(priority = -1) { event ->
        if (mc.player == null || disablelogger || event.origin != TransferOrigin.SEND) {
            return@handler
        }

        if (ambush && event.packet is PlayerInteractEntityC2SPacket) {
            enabled = false

            return@handler
        }

        if (event.packet is PlayerMoveC2SPacket || event.packet is PlayerInteractBlockC2SPacket ||
            event.packet is HandSwingC2SPacket ||
            event.packet is PlayerActionC2SPacket || event.packet is PlayerInteractEntityC2SPacket
        ) {
            if (event.packet is PlayerMoveC2SPacket && !event.packet.changePosition) {
                return@handler
            }

            if (event.packet is PlayerMoveC2SPacket) {
                positionPackets.getAndIncrement()
                synchronized(positions) { positions.addAll(listOf(event.packet.x, event.packet.y, event.packet.z)) }
            }

            event.cancelEvent()

            packets.add(event.packet)
        }
    }

    val repeatable = repeatable {
        if (pulse) {
            wait(delay)
            blink()
        }
    }

    val playerMoveHandler = handler<PlayerMovementTickEvent> {
        if (AutoResetOption.enabled && positionPackets.get() > AutoResetOption.resetAfter) {
            reset()
            notification("Blink", "Auto reset", NotificationEvent.Severity.INFO)

            enabled = false
        }
    }

    private fun blink() {
        try {
            disablelogger = true

            while (!packets.isEmpty()) {
                network.sendPacket(packets.take())
            }

            disablelogger = false
        } catch (e: Exception) {
            e.printStackTrace()

            disablelogger = false
        }

        synchronized(positions) { positions.clear() }
        positionPackets.set(0)
    }

    private fun reset() {
        this.packets.clear()
        synchronized(positions) { positions.clear() }
        positionPackets.set(0)

        val start = startPos!!

        player.setPos(start.x, start.y, start.z)

        player.prevX = start.x
        player.prevY = start.y
        player.prevZ = start.z
        player.lastRenderX = start.x
        player.lastRenderY = start.y
        player.lastRenderZ = start.z

        player.setVelocity(0.0, 0.0, 0.0)

        player.updatePositionAndAngles(start.x, start.y, start.z, player.yaw, player.pitch)
    }
}
