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
package net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
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
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.getDamageFromExplosion
import net.ccbluex.liquidbounce.utils.entity.getEffectiveDamage
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import java.util.*

/**
 * Fake Player Command
 *
 * Allows you to spawn a client side player for testing purposes.
 */
object CommandFakePlayer : CommandFactory, EventListener {

    /**
     * Stores all fake players.
     */
    private val fakePlayers = ArrayList<FakePlayer>()

    private var recording = false
    private val snapshots = ArrayList<PosPoseSnapshot>()

    // the entity ids of fake players shouldn't conflict with real entity ids, so they are negative
    private var fakePlayerId = -1

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("fakeplayer")
            .requiresIngame()
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
                    mc.inGameHud.chatHud.removeMessage("CFakePlayer#info")
                    val data = MessageMetadata(id = "CFakePlayer#info", remove = false)

                    chat(warning((command.result("noFakePlayerNamed", name))), metadata = data)
                    chat(regular(command.result("currentlySpawned")), metadata = data)
                    fakePlayers.forEach { fakePlayer ->
                        chat(regular("- " + fakePlayer.name.string), metadata = data)
                    }

                    return@handler
                }

                playersToRemove.forEach { fakePlayer ->
                    world.removeEntity(fakePlayer.id, Entity.RemovalReason.KILLED)
                    chat(
                        regular(
                            command.result(
                                "fakePlayerRemoved",
                                fakePlayer.x.roundToDecimalPlaces(),
                                fakePlayer.y.roundToDecimalPlaces(),
                                fakePlayer.z.roundToDecimalPlaces()
                            )
                        ),
                        metadata = MessageMetadata(id = "CFakePlayer#info")
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
                chat(
                    regular(command.result("startedRecording")),
                    metadata = MessageMetadata(id = "CFakePlayer#info")
                )
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
                snapshots = snapshots.toTypedArray(),
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
                    fakePlayer.x.roundToDecimalPlaces(),
                    fakePlayer.y.roundToDecimalPlaces(),
                    fakePlayer.z.roundToDecimalPlaces()
                )
            ),
            metadata = MessageMetadata(id = "CFakePlayer#info")
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
            fakePlayers.forEach { fakePlayer ->
                val damage = fakePlayer.getDamageFromExplosion(
                    pos = packet.center // will only work for crystals
                )

                val absorption = fakePlayer.absorptionAmount
                fakePlayer.health -= damage - absorption
                fakePlayer.absorptionAmount -= damage.coerceAtMost(absorption)
            }
        }

        /**
         * The server should not know that we tried to attack a fake player.
         */
        if (
            packet is PlayerInteractEntityC2SPacket &&
            fakePlayers.any { fakePlayer ->
                packet.entityId == fakePlayer.id
            }
        ) {
            it.cancelEvent()
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (event.isCancelled) {
            return@handler
        }

        if (fakePlayers.isEmpty()) {
            return@handler
        }

        val contains = fakePlayers.any { player ->
            player.id == event.entity.id
        }

        if (!contains) {
            return@handler
        }

        val fakePlayer = event.entity as LivingEntity

        val genericAttackDamage = if (player.isUsingRiptide) {
                player.riptideAttackDamage
            } else {
                player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE).toFloat()
            }
        val damageSource = player.damageSources.playerAttack(player)
        var enchantAttackDamage = player.getDamageAgainst(fakePlayer, genericAttackDamage,
            damageSource) - genericAttackDamage

        val attackCooldown = player.getAttackCooldownProgress(0.5f)
        enchantAttackDamage *= attackCooldown
        val damage = fakePlayer.getEffectiveDamage(damageSource, enchantAttackDamage, false)

        val absorption = fakePlayer.absorptionAmount
        fakePlayer.health -= damage - absorption
        fakePlayer.absorptionAmount -= damage.coerceAtMost(absorption)
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
            chat(
                markAsError(translation("liquidbounce.command.fakeplayer.recordingForTooLong")),
                metadata = MessageMetadata(id = "CFakePlayer#info")
            )
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

}
