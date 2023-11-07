package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePingSpoof
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.minecraft.entity.Entity
import net.minecraft.entity.TrackedPosition
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.sound.SoundEvents
import java.util.concurrent.CopyOnWriteArrayList

@Suppress("detekt:all")

object ModuleBacktrack : Module("Backtrack", Category.COMBAT) {

    val range by floatRange("Range", 1f..3f, 0f..6f)
    val delay by int("Delay", 100, 0..1000)

    //val boxColor by color("BoxColor", Color4b(0, 0, 0, 127))

    val packetQueue = CopyOnWriteArrayList<ModulePingSpoof.DelayData>()

    var target: Entity? = null
    var position: TrackedPosition? = null

    val packetHandler = handler<PacketEvent> {
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

        if (packet is EntityS2CPacket) {
            val entity = packet.getEntity(world)

            if (entity != null && entity == target) {
                // TODO: Properly add boxes from entities, this below does not work
                /*
                    if (position == null) {
                        position = TrackedPosition()

                        position?.setPos(entity.trackedPosition.pos)
                    }

                    position?.setPos(
                        position?.withDelta(
                            packet.deltaX.toLong(),
                            packet.deltaY.toLong(),
                            packet.deltaZ.toLong()
                        )
                    )*/
            }
        }

        packetQueue.add(ModulePingSpoof.DelayData(packet, System.currentTimeMillis(), System.nanoTime()))
    }

    /*
    val renderHandler = handler<WorldRenderEvent> { event ->
        val entity = target ?: return@handler

        val pos = position?.pos?.let {
            Vec3(it.x, it.y, it.z)
        } ?: return@handler

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
    }*/

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

        // Reset on enemy change
        if (enemy != target) {
            clear()
        }

        target = enemy
    }

    val toggleHandler = handler<ToggleModuleEvent>(ignoreCondition = true) {
        clear(!it.newState)
    }

    private fun processPackets(clear: Boolean = false) {
        val filtered = packetQueue.filter {
            clear || it.delay <= System.currentTimeMillis() - delay
        }.sortedBy { it.registration }

        for (data in filtered) {
            handlePacket(data.packet)

            packetQueue.remove(data)
        }
    }

    fun clear(handlePackets: Boolean = true, clearOnly: Boolean = false) {
        if (handlePackets && !clearOnly) {
            processPackets(true)
        } else if (clearOnly) {
            packetQueue.clear()
        }

        target = null
        position = null
    }

    private fun shouldCancelPackets() =
        target != null && target!!.isAlive && target!!.boxedDistanceTo(player) in range && player.age > 10
}
