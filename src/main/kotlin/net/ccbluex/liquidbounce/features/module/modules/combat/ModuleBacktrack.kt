package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePingSpoof
import net.ccbluex.liquidbounce.render.drawSolidBox
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.render.withPosition
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.minecraft.entity.Entity
import net.minecraft.entity.TrackedPosition
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object ModuleBacktrack : Module("Backtrack", Category.COMBAT) {

    val range by floatRange("Range", 1f..3f, 0f..6f)
    val delay by int("Delay", 100, 0..1000)
    private val boxColor by color("BoxColor", Color4b(36, 32, 147, 87))

    private val packetQueue = LinkedHashSet<ModulePingSpoof.DelayData>()

    var target: Entity? = null

    var position: TrackedPosition? = null

    val packetHandler = handler<PacketEvent> {
        synchronized(packetQueue) {
            if (it.origin != TransferOrigin.RECEIVE || it.isCancelled || packetQueue.isEmpty() && !shouldCancelPackets()) {
                return@handler
            }

            val packet = it.packet

            when (packet) {
                // Ignore chat packets
                is ChatMessageS2CPacket -> {
                    return@handler
                }

                // Flush on teleport or disconnect
                is PlayerPositionLookS2CPacket, is DisconnectS2CPacket -> {
                    clear(true)
                    return@handler
                }

                is PlaySoundS2CPacket -> {
                    if (packet.sound.value() == SoundEvents.ENTITY_PLAYER_HURT) {
                        return@handler
                    }
                }

                // Flush on own death
                is HealthUpdateS2CPacket -> {
                    if (packet.health <= 0) {
                        clear(true)
                        return@handler
                    }
                }
            }

            it.cancelEvent()

            // Update box position with these packets
            if (packet is EntityS2CPacket && packet.getEntity(world) == target || packet is EntityPositionS2CPacket && packet.id == target?.id) {
                val pos = if (packet is EntityS2CPacket) {
                    position?.withDelta(packet.deltaX.toLong(), packet.deltaY.toLong(), packet.deltaZ.toLong())
                } else {
                    (packet as EntityPositionS2CPacket).let { vec -> Vec3d(vec.x, vec.y, vec.z) }
                }

                if (player.squaredDistanceTo(pos) <= player.squaredDistanceTo(target)) {
                    clear(true)
                }
                position?.setPos(pos)
            }

            packetQueue.add(ModulePingSpoof.DelayData(packet, System.currentTimeMillis()))
        }
    }


    val renderHandler = handler<WorldRenderEvent> { event ->
        val entity = target ?: return@handler

        val pos = Vec3(position?.pos ?: return@handler)

        val dimensions = entity.getDimensions(entity.pose)

        val d = dimensions.width.toDouble() / 2.0

        val box = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)

        renderEnvironmentForWorld(event.matrixStack) {
            val color = boxColor

            withPosition(pos) {
                withColor(color) {
                    drawSolidBox(box)
                }
            }
        }
    }

    val tickHandler = handler<GameTickEvent> {
        if (shouldCancelPackets()) {
            processPackets()
        } else {
            clear()
        }
    }

    val worldChangeHandler = handler<WorldChangeEvent> {
        // Clear packets on disconnect only
        if (it.world == null) {
            clear(clearOnly = true)
        }
    }

    val attackHandler = handler<AttackEvent> {
        val enemy = it.enemy

        if (!enemy.shouldBeAttacked())
            return@handler

        // Reset on enemy change
        if (enemy != target) {
            clear()

            // Instantly set new position, so it does not look like the box was created with delay
            position = TrackedPosition().apply { this.pos = enemy.trackedPosition.pos }
        }

        target = enemy
    }

    override fun enable() {
        clear(false)
    }

    override fun disable() {
        clear(true)
    }

    private fun processPackets(clear: Boolean = false) {
        synchronized(packetQueue) {
            packetQueue.removeIf {
                if (clear || it.delay <= System.currentTimeMillis() - delay) {
                    handlePacket(it.packet)
                    return@removeIf true
                }

                false
            }
        }
    }

    fun clear(handlePackets: Boolean = true, clearOnly: Boolean = false) {
        if (handlePackets && !clearOnly) {
            processPackets(true)
        } else if (clearOnly) {
            synchronized(packetQueue) {
                packetQueue.clear()
            }
        }

        target = null
        position = null
    }

    private fun shouldCancelPackets() =
        target != null && target!!.isAlive && target!!.boxedDistanceTo(player) in range && player.age > 10
}
