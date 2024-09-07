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
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.world.explosion.Explosion
import net.minecraft.world.explosion.ExplosionBehavior
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Fake Player Command
 *
 * Allows you to spawn a client side player for testing purposes.
 */
@IncludeCommand
object CommandFakePlayer : CommandFactory, Listenable {

    /**
     * Stores all fake players.
     */
    private val fakePlayers = ArrayList<FakePlayer>()

    private var recording = false
    private val snapshots = ArrayList<PosPoseSnapshot>()

    private val explosionBehavior: ExplosionBehavior = ExplosionBehavior()

    // the entity ids of fake players shouldn't conflict with real entity ids, so they are negative
    private var fakePlayerId = -1

    override fun createCommand(): Command {
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

    private fun spawnCommand(): Command {
        return CommandBuilder
            .begin("spawn")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .optional()
                    .build()
            )
            .handler { _, args ->
                checkInGame()
                spawn(args, false)
            }
            .build()
    }

    private fun removeCommand(): Command {
        return CommandBuilder
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

                if (fakePlayers.isEmpty()) {
                    throw CommandException(translation("liquidbounce.command.fakeplayer.noFakePlayers"))
                }

                val name = args.getOrNull(0)?.toString() ?: "FakePlayer"

                val playersToRemove = fakePlayers.filter { fakePlayer -> fakePlayer.name.string == name }

                if (playersToRemove.isEmpty()) {
                    chat(warning((command.result("noFakePlayerNamed", name))))
                    chat(regular(command.result("currentlySpawned")))
                    fakePlayers.forEach { fakePlayer ->
                        chat(regular("- " + fakePlayer.name.string))
                    }

                    return@handler
                }

                playersToRemove.forEach { fakePlayer ->
                    world.removeEntity(fakePlayer.id, Entity.RemovalReason.KILLED)
                    chat(
                        regular(
                            command.result(
                                "fakePlayerRemoved",
                                roundToDecimalPlaces(fakePlayer.x),
                                roundToDecimalPlaces(fakePlayer.y),
                                roundToDecimalPlaces(fakePlayer.z)
                            )
                        )
                    )
                }

                fakePlayers.removeAll(playersToRemove.toSet())
            }
            .build()
    }

    private fun clearCommand(): Command {
        return CommandBuilder
            .begin("clear")
            .handler { _, _ ->
                checkInGame()

                if (fakePlayers.isEmpty()) {
                    throw CommandException(translation("liquidbounce.command.fakeplayer.noFakePlayers"))
                }

                fakePlayers.forEach { fakePlayer ->
                    world.removeEntity(fakePlayer.id, Entity.RemovalReason.DISCARDED)
                }
                fakePlayers.clear()
            }
            .build()
    }

    @Suppress("SpellCheckingInspection")
    private fun startRecordingCommand(): Command {
        return CommandBuilder
            .begin("startrecording")
            .handler { command, _ ->
                checkInGame()

                if (recording) {
                    throw CommandException(command.result("alreadyRecording"))
                }

                recording = true
                chat(regular(command.result("startedRecording")))
                notification(
                    "FakePlayer",
                    command.result("startedRecordingNotification"),
                    NotificationEvent.Severity.INFO
                )
            }
            .build()
    }

    @Suppress("SpellCheckingInspection")
    private fun endRecordingCommand(): Command {
        return CommandBuilder
            .begin("endrecording")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .optional()
                    .build()
            )
            .handler { command, args ->
                checkInGame()

                if (!recording) {
                    throw CommandException(command.result("notRecording"))
                }

                if (snapshots.isEmpty()) {
                    throw CommandException(command.result("somethingWentWrong"))
                }

                spawn(args, true)
                stopRecording()
            }
            .build()
    }

    /**
     * Adds a new fake player.
     *
     * Note: a moving player requires [snapshots] to be not null.
     *
     * @param moving true if the fake player should play a recording.
     */
    private fun spawn(args: Array<Any>, moving: Boolean) {
        val nameArg = args.getOrNull(0)?.toString() ?: "FakePlayer"
        val fakePlayer: FakePlayer

        if (moving) {
            fakePlayer = MovingFakePlayer(
                snapshots = this.snapshots.map { it }.toTypedArray(),
                world,
                GameProfile(
                    UUID.randomUUID(),
                    nameArg
                ),
            ).apply {
                onRemoval = { fakePlayers.remove(this) }
            }
        } else {
            fakePlayer = FakePlayer(
                world,
                GameProfile(
                    UUID.randomUUID(),
                    nameArg
                )
            ).apply {
                onRemoval = { fakePlayers.remove(this) }
            }
        }

        fakePlayer.id = fakePlayerId
        fakePlayerId--

        if (!moving) {
            fakePlayer.loadAttributes(fromPlayer(player))
        }

        fakePlayers.add(fakePlayer)
        world.addEntity(fakePlayer)
        chat(
            regular(
                translation(
                    "liquidbounce.command.fakeplayer.fakePlayerSpawned",
                    roundToDecimalPlaces(fakePlayer.x),
                    roundToDecimalPlaces(fakePlayer.y),
                    roundToDecimalPlaces(fakePlayer.z)
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

    @Suppress("unused")
    val explosionHandler = handler<PacketEvent> {
        if (fakePlayers.isEmpty()) {
            return@handler
        }

        val packet = it.packet

        /**
         * Explosions are not handled by [LivingEntity#damage]
         * so an ExplosionS2CPacket handler is required.
         */
        if (packet is ExplosionS2CPacket) {
            val explosion = Explosion(
                world,
                null,
                packet.x,
                packet.y,
                packet.z,
                packet.radius,
                packet.affectedBlocks,
                packet.destructionType,
                packet.particle,
                packet.emitterParticle,
                packet.soundEvent
            )

            fakePlayers.forEach { fakePlayer ->
                if (!explosionBehavior.shouldDamage(explosion, fakePlayer)) { // might not be necessary
                    return@handler
                }

                fakePlayer.damage(
                    Explosion.createDamageSource(world, null),
                    explosionBehavior.calculateDamage(explosion, fakePlayer)
                )
            }
        }

        /**
         * The server should not know that we tried to attack a fake player.
         */
        if (
            packet is PlayerInteractEntityC2SPacket &&
            fakePlayers.any { fakePlayers ->
                packet.entityId == fakePlayers.id
            }
        ) {
            it.cancelEvent()
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

        if (snapshots.size >= Int.MAX_VALUE - 1) {
            chat(markAsError(translation("liquidbounce.command.fakeplayer.recordingForTooLong")))
            stopRecording()
            return@handler
        }

        snapshots.add(fromPlayerMotion(player))
    }

    /**
     * Stops recording and notifies the user about it.
     */
    private fun stopRecording() {
        recording = false
        snapshots.clear()
        notification(
            "FakePlayer",
            translation("liquidbounce.command.fakeplayer.stoppedRecording"),
            NotificationEvent.Severity.INFO
        )
    }

    /**
     * Rounds the given number to the specified decimal place (the first by default).
     * For additional info see [RoundingMode#HALF_UP].
     *
     * For example ```roundToNDecimalPlaces(1234.567,decimalPlaces=1)``` will
     * return ```1234.6```.
     */
    private fun roundToDecimalPlaces(number: Double, decimalPlaces: Int = 1): Double {
        return BigDecimal(number).setScale(decimalPlaces, RoundingMode.HALF_UP).toDouble()
    }

}
