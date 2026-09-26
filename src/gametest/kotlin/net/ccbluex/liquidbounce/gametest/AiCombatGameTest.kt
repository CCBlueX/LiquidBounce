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
package net.ccbluex.liquidbounce.gametest

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.combat.CombatController
import net.ccbluex.liquidbounce.deeplearn.combat.CombatPackets
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.PositionPath
import net.minecraft.world.phys.Vec3
import org.apache.logging.log4j.LogManager
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fights a scripted opponent that circles the player with KillAura's AI aiming and clicking on, using the bundled
 * model, so the whole path from packets to decisions to rotations and attacks runs in a real client.
 */
class AiCombatGameTest : FabricClientGameTest {
    private val logger = LogManager.getLogger("AiCombatGameTest")

    override fun runTest(context: ClientGameTestContext) {
        context.waitFor({ ScreenManager.mainBrowser != null && DeepLearningEngine.isInitialized }, 2400)
        context.worldBuilder().setUseConsistentSettings(true).create().use { world ->
            world.connection.waitForChunksRender()
            context.waitFor { it.player?.onGround() == true }
            world.server.runOnServer<RuntimeException> { server ->
                server.commands.performPrefixedCommand(server.createCommandSourceStack(), "gamemode creative @a")
                server.commands.performPrefixedCommand(server.createCommandSourceStack(),
                    "item replace entity @a weapon.mainhand with minecraft:diamond_sword")
            }
            context.waitTicks(5)
            context.client {
                ModuleManager.filter { it.enabled && it !== ModuleHud }.forEach { it.enabled = false }
                KillAuraRotationsValueGroup.get().first { it.name == "AngleSmooth" }.setByString("AI")
                ModuleKillAura.clicker.get().first { it.name == "Technique" }.setByString("AI")
            }
            fight(context)
        }
    }

    private fun fight(context: ClientGameTestContext) {
        val attacks = AttackCounter()
        val center = context.client { it.player!!.position() }
        val opponent = context.client { client ->
            RemotePlayer(client.level!!, GameProfile(UUID.nameUUIDFromBytes(NAME.toByteArray()), NAME)).apply {
                id = OPPONENT_ID
                setPos(circle(center, 0))
                client.level!!.addEntity(this)
            }
        }
        var decisions = 0
        var travel = 0f
        var lastYaw: Float? = null
        context.client { ModuleKillAura.enabled = true }
        try {
            context.client { check(CombatPackets.running) { "KillAura AI does not capture packets" } }
            for (tick in 0 until TICKS) {
                context.client { client ->
                    client.receive(ClientboundEntityPositionSyncPacket(opponent.id,
                        PositionPath.of(circle(center, tick)), tick * STEP + 90f, 0f, true))
                }
                context.waitTick()
                context.client { client ->
                    if (CombatController.lastDecision?.takeIf { it.heads.aim && it.heads.attacks } != null) {
                        decisions++
                    }
                    val yaw = RotationManager.currentRotation?.yaw ?: client.player!!.yRot
                    lastYaw?.let { travel += abs(Mth.wrapDegrees(yaw - it)) }
                    lastYaw = yaw
                }
            }
        } finally {
            context.client { client ->
                ModuleKillAura.enabled = false
                attacks.close()
                client.level!!.removeEntity(opponent.id, Entity.RemovalReason.DISCARDED)
            }
        }
        logger.info("Fight: decisions={} travel={} attacks={}", decisions, travel, attacks.count)
        check(decisions > 0) { "The bundled model made no decision with aim and attacks" }
        check(travel > 20f) { "AI aiming barely moved: $travel degrees" }
        check(attacks.count > 0) { "AI clicking never attacked" }
        logger.info("PASS: AI aiming followed the opponent and attacked")
    }

    private fun circle(center: Vec3, tick: Int): Vec3 {
        val angle = Math.toRadians((tick * STEP).toDouble())
        return center.add(-sin(angle) * RADIUS, 0.0, cos(angle) * RADIUS)
    }

    private fun <T> ClientGameTestContext.client(block: (Minecraft) -> T): T =
        computeOnClient<T, RuntimeException> { block(it) }

    /** Hands [packet] to the client like the server connection would, and returns once Netty queued it. */
    private fun Minecraft.receive(packet: Packet<*>) {
        val channel = connection!!.connection.channel
        channel.eventLoop().submit { channel.pipeline().fireChannelRead(packet) }.get()
    }

    private class AttackCounter : EventListener {
        var count = 0
            private set

        @Suppress("unused")
        private val handler = handler<AttackEntityEvent> { if (!it.isCancelled) count++ }

        fun close() = unregister()
    }

    private companion object {
        const val NAME = "Opponent"
        const val OPPONENT_ID = -3001
        const val TICKS = 300
        const val RADIUS = 2.5
        const val STEP = 6f
    }
}
