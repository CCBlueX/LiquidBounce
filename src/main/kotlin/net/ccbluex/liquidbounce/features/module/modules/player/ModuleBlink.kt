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

import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBreadcrumbs
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.play.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

object ModuleBlink : Module("Blink", Category.PLAYER) {
    private val pulse by boolean("Pulse", false)
    private val delay by int("Delay", 20, 10..100)
    private val breadcrumbs by boolean("Breadcrumbs", false)
    private val breadcrumbscolor by color("BreadcrumbsColor", Color4b(255, 179, 72, 255))
    private val breadcrumbsrainbow by boolean("BreadcrumbsRainbow", false)

    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var fakeplayer: OtherClientPlayerEntity? = null
    private var disablelogger = false
    private val positions = mutableListOf<Double>()

    override fun enable() {
        if (!pulse) {
            val faker = OtherClientPlayerEntity(world, player.gameProfile)

            faker.headYaw = player.headYaw
            faker.copyPositionAndRotation(player)
            world.addEntity(faker.entityId, faker)

            fakeplayer = faker
        }
    }

    val renderHandler = handler<EngineRenderEvent> {
        val color = if (breadcrumbsrainbow) rainbow() else breadcrumbscolor

        synchronized(positions) {
            if (breadcrumbs) {
                RenderEngine.enqueueForRendering(
                    RenderEngine.CAMERA_VIEW_LAYER,
                    ModuleBreadcrumbs.createBreadcrumbsRenderTask(color, this.positions)
                )
            }
        }
    }

    override fun disable() {
        if (mc.player == null)
            return

        blink()

        removeFakePlayer()
    }

    private fun removeFakePlayer() {
        val faker = this.fakeplayer

        if (faker != null) {
            world.removeEntity(faker.entityId)

            this.fakeplayer = null
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (mc.player == null || disablelogger)
            return@handler

        if (event.packet is PlayerMoveC2SPacket || event.packet is PlayerInteractBlockC2SPacket ||
            event.packet is HandSwingC2SPacket ||
            event.packet is PlayerActionC2SPacket || event.packet is PlayerInteractEntityC2SPacket
        ) {
            if (event.packet is PlayerMoveC2SPacket && event.packet.changePosition) {
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
    }
}
