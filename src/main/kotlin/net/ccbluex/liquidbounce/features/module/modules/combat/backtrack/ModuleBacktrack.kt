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
package net.ccbluex.liquidbounce.features.module.modules.combat.backtrack

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.drawSolidBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.PacketSnapshot
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.squareBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.TrackedPosition
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object ModuleBacktrack : ClientModule("Backtrack", Category.COMBAT) {

    private val range by floatRange("Range", 1f..3f, 0f..10f)
    val delay by intRange("Delay", 100..150, 0..1000, "ms")
    private val nextBacktrackDelay by intRange("NextBacktrackDelay", 0..10, 0..2000, "ms")
    private val trackingBuffer by int("TrackingBuffer", 500, 0..2000, "ms")
    private val chance by float("Chance", 50f, 0f..100f, "%")
    private var currentChance = (0..100).random()

    private object PauseOnHurtTime : ToggleableConfigurable(this, "PauseOnHurtTime", false) {
        val hurtTime by int("HurtTime", 3, 0..10)
    }

    private val pauseOnHurtTime = tree(PauseOnHurtTime)

    private val targetMode by enumChoice("TargetMode", Mode.ATTACK)
    private val lastAttackTimeToWork by int("LastAttackTimeToWork", 1000, 0..5000)

    enum class Mode(override val choiceName: String) : NamedChoice {
        ATTACK("Attack"),
        RANGE("Range")
    }

    private val espMode = choices(
        "EspMode", Wireframe, arrayOf(
            Box, Model, Wireframe, None
        )
    ).apply {
        doNotIncludeAlways()
    }

    val delayedPacketQueue = Queues.newConcurrentLinkedQueue<PacketSnapshot>()
    val packetProcessQueue = Queues.newConcurrentLinkedQueue<Packet<*>>()

    private val chronometer = Chronometer()
    private val trackingBufferChronometer = Chronometer()
    private val attackChronometer = Chronometer()

    private var shouldPause = false

    var target: Entity? = null
    private var position: TrackedPosition? = null

    var currentDelay = delay.random()

    val arePacketQueuesEmpty
        get() = delayedPacketQueue.isEmpty() && packetProcessQueue.isEmpty()

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING || event.isCancelled) {
            return@handler
        }

        if (arePacketQueuesEmpty && !shouldCancelPackets()) {
            return@handler
        }

        val packet = event.packet

        when (packet) {
            // Ignore message-related packets
            is ChatMessageC2SPacket, is GameMessageS2CPacket, is CommandExecutionC2SPacket -> {
                return@handler
            }

            // Flush on teleport or disconnect
            is PlayerPositionLookS2CPacket, is DisconnectS2CPacket -> {
                clear(true)
                return@handler
            }

            // Ignore own hurt sounds
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

        // Update box position with these packets
        val entityPacket = packet is EntityS2CPacket && packet.getEntity(world) == target
        val positionPacket = packet is EntityPositionS2CPacket && packet.entityId == target?.id
        val syncPacket = packet is EntityPositionSyncS2CPacket && packet.id == target?.id
        if (entityPacket || positionPacket || syncPacket) {
            val pos = if (packet is EntityS2CPacket) {
                position?.withDelta(packet.deltaX.toLong(), packet.deltaY.toLong(), packet.deltaZ.toLong())
            } else if (packet is EntityPositionS2CPacket) {
                Vec3d(packet.change.position.x, packet.change.position.y, packet.change.position.z)
            } else {
                (packet as EntityPositionSyncS2CPacket).values.position()
            }

            position?.setPos(pos)

            // Is the target's actual position closer than its tracked position?
            if (target!!.squareBoxedDistanceTo(player, pos!!) < target!!.squaredBoxedDistanceTo(player)) {
                // Process all packets. We want to be able to hit the enemy, not the opposite.
                processPackets(true)
                // And stop right here. No need to cancel further packets.
                return@handler
            }
        }

        event.cancelEvent()
        delayedPacketQueue.add(PacketSnapshot(packet, event.origin, System.currentTimeMillis()))
    }

    abstract class RenderChoice(name: String) : Choice(name) {
        protected fun getEntityPosition(): Pair<Entity, Vec3d>? {
            val entity = target ?: return null
            val pos = position?.pos ?: return null
            return entity to pos
        }
    }

    object Box : RenderChoice("Box") {
        override val parent: ChoiceConfigurable<RenderChoice>
            get() = espMode

        private val color by color("Color", Color4b(36, 32, 147, 87))

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            val (entity, pos) = getEntityPosition() ?: return@handler

            val dimensions = entity.getDimensions(entity.pose)
            val d = dimensions.width.toDouble() / 2.0

            val box = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)

            renderEnvironmentForWorld(event.matrixStack) {
                withPositionRelativeToCamera(pos) {
                    withColor(color) {
                        drawSolidBox(box)
                    }
                }
            }
        }
    }

    object Model : RenderChoice("Model") {
        override val parent: ChoiceConfigurable<RenderChoice>
            get() = espMode

        private val lightAmount by float("LightAmount", 0.3f, 0.01f..1f)

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            val (entity, pos) = getEntityPosition() ?: return@handler

            val light = world.getLightLevel(BlockPos.ORIGIN)
            val reducedLight = (light * lightAmount.toDouble()).toInt()

            renderEnvironmentForWorld(event.matrixStack) {
                withPositionRelativeToCamera(pos) {
                    mc.entityRenderDispatcher.render(
                        entity,
                        0.0,
                        0.0,
                        0.0,
                        1f,
                        event.matrixStack,
                        mc.bufferBuilders.entityVertexConsumers,
                        reducedLight
                    )
                }
            }
        }
    }

    object Wireframe : RenderChoice("Wireframe") {
        override val parent: ChoiceConfigurable<RenderChoice>
            get() = espMode

        private val color by color("Color", Color4b(36, 32, 147, 87))
        private val outlineColor by color("OutlineColor", Color4b(36, 32, 147, 255))

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> {
            val (entity, pos) = getEntityPosition() ?: return@handler

            val wireframePlayer = WireframePlayer(pos, entity.yaw, entity.pitch)
            wireframePlayer.render(it, color, outlineColor)
        }
    }

    object None : RenderChoice("None") {
        override val parent: ChoiceConfigurable<RenderChoice>
            get() = espMode
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> { event ->
        // Clear packets on disconnect only
        if (event.world == null) {
            clear(clearOnly = true)
        }
    }

    private fun getTargetEntity(): Entity? {
        return when (targetMode) {
            Mode.ATTACK -> null // the attack handler will handle this
            Mode.RANGE -> world.findEnemy(range)
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        attackChronometer.reset() // Update the last attack time
        currentChance = (0..100).random()

        if (targetMode != Mode.ATTACK) {
            return@handler
        }

        val enemy = event.entity
        processTarget(enemy)
    }

    @Suppress("unused")
    private val rangeTargetHandler = tickHandler {
        if (targetMode != Mode.RANGE) return@tickHandler

        val enemy = getTargetEntity()
        if (enemy == null) {
            clear()
            return@tickHandler
        }

        processTarget(enemy)
    }

    private fun processTarget(enemy: Entity) {
        shouldPause = enemy is LivingEntity && enemy.hurtTime >= PauseOnHurtTime.hurtTime

        if (!shouldBacktrack(enemy)) {
            return
        }

        // Reset on enemy change
        if (enemy != target) {
            clear(resetChronometer = false)

            // Instantly set new position, so it does not look like the box was created with delay
            position = TrackedPosition().apply { this.pos = enemy.trackedPosition.pos }
        }

        target = enemy
    }

    override fun onEnabled() {
        clear(false)
    }

    override fun onDisabled() {
        clear(true)
    }

    fun processPackets(clear: Boolean = false) {
        delayedPacketQueue.removeIf {
            if (clear || it.timestamp <= System.currentTimeMillis() - currentDelay) {
                packetProcessQueue.add(it.packet)
                return@removeIf true
            }
            false
        }
    }

    fun clear(handlePackets: Boolean = true, clearOnly: Boolean = false, resetChronometer: Boolean = true) {
        if (handlePackets && !clearOnly) {
            processPackets(true)
        } else if (clearOnly) {
            delayedPacketQueue.clear()
        }

        if (target != null && resetChronometer) {
            chronometer.waitForAtLeast(nextBacktrackDelay.random().toLong())
        }

        target = null
        position = null
    }

    private fun shouldBacktrack(target: Entity): Boolean {
        val inRange = target.boxedDistanceTo(player) in range

        if (inRange) {
            trackingBufferChronometer.reset()
        }

        return (inRange || !trackingBufferChronometer.hasElapsed(trackingBuffer.toLong())) &&
            target.shouldBeAttacked() &&
            player.age > 10 &&
            currentChance < chance &&
            chronometer.hasElapsed() &&
            !shouldPause() &&
            !attackChronometer.hasElapsed(lastAttackTimeToWork.toLong())
    }

    fun isLagging() = running && !arePacketQueuesEmpty

    private fun shouldPause() = pauseOnHurtTime.enabled && shouldPause

    fun shouldCancelPackets() =
        target?.let { target -> target.isAlive && shouldBacktrack(target) } == true
}
