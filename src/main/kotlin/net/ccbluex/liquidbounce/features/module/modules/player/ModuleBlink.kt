package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable
import net.ccbluex.liquidbounce.renderer.engine.*
import net.ccbluex.liquidbounce.renderer.utils.rainbow
import net.ccbluex.liquidbounce.utils.MSTimer
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.play.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

object ModuleBlink : Module("Blink", Category.PLAYER) {
    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var fakeplayer: OtherClientPlayerEntity? = null
    private var disablelogger = false
    private val positions = mutableListOf<Double>()
    private val pulse by boolean("Pulse", false)
    private val pulsedelay by int("PulseDelay", 1000, 500..5000)
    private val breadcrumbs by boolean("Breadcrumbs", false)
    private val breadcrumbscolor by color("BreadcrumbsColor", Color4b(255, 179, 72, 255))
    private val breadcrumbsrainbow by boolean("BreadcrumbsRainbow", false)
    private val pulsetimer = MSTimer()

    override fun enable() {
        if (!pulse) {
            val faker = OtherClientPlayerEntity(world, player.gameProfile)

            faker.headYaw = player.headYaw
            //faker.renderYawOffset = player.renderYawOffset
            faker.copyPositionAndRotation(player)
            faker.headYaw = player.headYaw
            world.addEntity(-1337, faker)

            fakeplayer = faker
        }
        synchronized(positions) {
            positions.addAll(listOf(player.x, player.eyeY, player.z))
            positions.addAll(listOf(player.x, player.y, player.z))
        }
        pulsetimer.reset()
    }

    val renderHandler = handler<EngineRenderEvent> {
        val color = if (breadcrumbsrainbow) rainbow() else breadcrumbscolor
        synchronized(positions) {
            if (breadcrumbs) {
                val renderTask = ColoredPrimitiveRenderTask(this.positions.size, PrimitiveType.LineStrip)

                for (i in 0 until this.positions.size / 3) {
                    renderTask.index(
                        renderTask.vertex(
                            Vec3(
                                positions[i * 3],
                                positions[i * 3 + 1],
                                positions[i * 3 + 2]
                            ), color
                        )
                    )
                }

                RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, renderTask)
            }
        }
    }

    override fun disable() {
        if (mc.player == null)
            return

        blink()

        val faker = fakeplayer

        if (faker != null) {
            world.removeEntity(faker.entityId)
            fakeplayer = null
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (mc.player == null || disablelogger)
            return@handler

        if (event.packet is PlayerMoveC2SPacket) // Cancel all movement stuff
            event.cancelEvent()

        if (event.packet is PlayerMoveC2SPacket.PositionOnly || event.packet is PlayerMoveC2SPacket.LookOnly ||
            event.packet is PlayerInteractBlockC2SPacket ||
            event.packet is HandSwingC2SPacket ||
            event.packet is PlayerActionC2SPacket || event.packet is PlayerInteractEntityC2SPacket
        ) {
            event.cancelEvent()
            packets.add(event.packet)
        }
    }

    val repeatable = repeatable {
        synchronized(positions) { positions.addAll(listOf(player.x, player.y, player.z)) }
        if (pulse && pulsetimer.hasTimePassed(pulsedelay.toLong())) {
            blink()
            pulsetimer.reset()
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
