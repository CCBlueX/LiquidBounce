/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.client.fakeplayer

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.combat.getDamageFromExplosion
import net.minecraft.entity.Entity
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.util.math.Vec3d
import net.minecraft.world.explosion.Explosion
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Fake Player Command
 *
 * Allows you to spawn a client side player for testing purposes.
 */
object CommandFakePlayer: Listenable {

    /**
     * Stores all fake players.
     */
    private var fakePlayers: MutableList<FakePlayer>? = null

    private var recording = false
    private var snapshots: MutableList<PosPoseSnapshot>? = null

    fun createCommand(): Command {
        return CommandBuilder
            .begin("fakeplayer")
            .hub()
            .subcommand(spawnCommand())
            .subcommand(removeCommand())
            .subcommand(clearCommand())
            .subcommand(startRecordingCommand())
            .subcommand(endRecordingCommand())
            .build()
    }

    private fun spawnCommand() = CommandBuilder
        .begin("spawn")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .optional()
                .build()
        )
        .handler { _, args ->
            spawn(args, false)
        }
        .build()

    private fun removeCommand() = CommandBuilder
        .begin("remove")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .optional()
                .build()
        )
        .handler { command, args ->
            checkInGame()

            if (fakePlayers == null || fakePlayers!!.isEmpty()) {
                throw CommandException(translation("liquidbounce.command.fakeplayer.noFakePlayers"))
            }

            val name = args.getOrNull(0)?.toString() ?: "FakePlayer"
            var anyRemoved = false

            fakePlayers?.removeIf { fakePlayer ->
                @Suppress("ReplaceCallWithBinaryOperator")
                val remove = name.equals(fakePlayer.name.string)
                if (remove) {
                    world.removeEntity(fakePlayer.id, Entity.RemovalReason.KILLED)
                    chat(
                        regular(
                            command.result(
                                "fakePlayerRemoved",
                                BigDecimal(fakePlayer.x).setScale(1, RoundingMode.HALF_UP).toDouble(),
                                BigDecimal(fakePlayer.y).setScale(1, RoundingMode.HALF_UP).toDouble(),
                                BigDecimal(fakePlayer.z).setScale(1, RoundingMode.HALF_UP).toDouble()
                            )
                        )
                    )
                    anyRemoved = true
                    return@removeIf true
                } else {
                    return@removeIf false
                }
            }

            if (!anyRemoved) {
                chat(warning((command.result("noFakePlayerNamed", name))))
                chat(regular(command.result("currentlySpawned")))
                fakePlayers!!.forEach { fakePlayer ->
                    chat(regular("- " + fakePlayer.name.string))
                }
            }
        }
        .build()

    private fun clearCommand() = CommandBuilder
        .begin("clear")
        .handler { _, _ ->
            checkInGame()

            if (fakePlayers == null || fakePlayers!!.isEmpty()) {
                throw CommandException(translation("liquidbounce.command.fakeplayer.noFakePlayers"))
            }

            fakePlayers!!.removeIf { fakePlayer ->
                world.removeEntity(fakePlayer.id, Entity.RemovalReason.DISCARDED)
                return@removeIf true
            }
            fakePlayers = null
        }
        .build()

    @Suppress("SpellCheckingInspection")
    private fun startRecordingCommand() = CommandBuilder
        .begin("startrecording")
        .handler { command, _ ->
            if (recording) {
                throw CommandException(command.result("alreadyRecording"))
            }

            recording = true
            snapshots = LinkedList<PosPoseSnapshot>()
            chat(regular(command.result("startedRecording")))
            notification(
                "FakePlayer",
                command.result("startedRecordingNotification"),
                NotificationEvent.Severity.INFO
            )
        }
        .build()

    @Suppress("SpellCheckingInspection")
    private fun endRecordingCommand() = CommandBuilder
        .begin("endrecording")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .optional()
                .build()
        )
        .handler { command, args ->
            if (!recording) {
                throw CommandException(command.result("notRecording"))
            }

            if (snapshots!!.isEmpty()) {
                throw CommandException(command.result("somethingWentWrong"))
            }

            spawn(args, true)
            stopRecording()
        }
        .build()

    /**
     * Adds a new fake player.
     *
     * Note: a moving player requires [snapshots] to be not null.
     *
     * @param moving true if the fake player should play a recording.
     */
    private fun spawn(args: Array<Any>, moving: Boolean) {
        checkInGame()

        if (fakePlayers == null) {
            fakePlayers = LinkedList<FakePlayer>()
        }

        val nameArg = args.getOrNull(0)?.toString() ?: "FakePlayer"
        val fakePlayer: FakePlayer

        if (moving) {
            fakePlayer = MovingFakePlayer(
                snapshots = this.snapshots!!.map { it }.toTypedArray(),
                world,
                GameProfile(
                    UUID.randomUUID(),
                    nameArg
                ),
            ) { thisFakePlayer -> fakePlayers?.remove(thisFakePlayer) }
        } else {
            fakePlayer = FakePlayer(
                world,
                GameProfile(
                    UUID.randomUUID(),
                    nameArg
                )
            ) { thisFakePlayer -> fakePlayers?.remove(thisFakePlayer) }
        }

        if (!moving) {
            fakePlayer.loadAttributes(fromPlayer(player))
        }

        fakePlayers!!.add(fakePlayer)
        world.addEntity(fakePlayer)
        chat(
            regular(
                translation(
                    "liquidbounce.command.fakeplayer.fakePlayerSpawned",
                    BigDecimal(fakePlayer.x).setScale(1, RoundingMode.HALF_UP).toDouble(),
                    BigDecimal(fakePlayer.y).setScale(1, RoundingMode.HALF_UP).toDouble(),
                    BigDecimal(fakePlayer.z).setScale(1, RoundingMode.HALF_UP).toDouble()
                )
            )
        )
    }

    /**
     * Verifies that the user is in a world and the player object exists.
     */
    private fun checkInGame() {
        if (mc.world == null || mc.player == null) {
            throw CommandException(translation("liquidbounce.command.fakeplayer.mustBeInGame"))
        }
    }

    /**
     * Explosions are not handled by [LivingEntity#damage]
     * so an ExplosionS2CPacket handler is required.
     */
    @Suppress("unused")
    val explosionHandler = handler<PacketEvent> {
        val packet = it.packet
        if (packet is ExplosionS2CPacket && fakePlayers != null) {
           fakePlayers!!.forEach { fakePlayer ->
               fakePlayer.damage(
                   Explosion.createDamageSource(world, player),
                   getDamageFromExplosion(
                       Vec3d(packet.x, packet.y, packet.z),
                       fakePlayer,
                       packet.radius
                   )
               )
           }
        }
    }

    /**
     * Recordings are made in the tick event handler.
     */
    @Suppress("unused")
    val tickHandler = handler<GameTickEvent> {
        if (!recording) {
            return@handler
        }

        if (mc.world == null || mc.player == null) {
            chat(markAsError(translation("liquidbounce.command.fakeplayer.mustBeInGame")))
            stopRecording()
            return@handler
        }

        if (snapshots!!.size >= Int.MAX_VALUE - 1) {
            chat(markAsError(translation("liquidbounce.command.fakeplayer.recordingForTooLong")))
            stopRecording()
            return@handler
        }

        snapshots!!.add(fromPlayerMotion(player))
    }

    /**
     * Stops recording and notifies the user about it.
     */
    private fun stopRecording() {
        recording = false
        snapshots = null
        notification("FakePlayer",
            translation("liquidbounce.command.fakeplayer.stoppedRecording"),
            NotificationEvent.Severity.INFO
        )
    }

}
