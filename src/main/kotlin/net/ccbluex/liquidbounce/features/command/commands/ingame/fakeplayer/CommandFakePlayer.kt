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
package net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.CommandDispatcher
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer.CommandFakePlayer.snapshots
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.removeMessage
import net.ccbluex.liquidbounce.utils.client.warning
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.getDamageFromExplosion
import net.ccbluex.liquidbounce.utils.entity.getEffectiveDamage
import net.ccbluex.liquidbounce.utils.math.roundToDecimalPlaces
import net.ccbluex.liquidbounce.utils.network.entityIdC2SInteractOrAttack
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import java.util.UUID

/**
 * Fake Player Command
 *
 * Allows you to spawn a client side player for testing purposes.
 */
@Suppress("detekt:TooManyFunctions")
object CommandFakePlayer : EventListener, CommandRegistrar {
    /**
     * Stores all fake players.
     */
    private val fakePlayers = ReferenceOpenHashSet<FakePlayer>()
    private val suggestions = suggestions<ClientCommandSource> { fakePlayers.map { it.name.string } }

    private var recording = false
    private val snapshots = ArrayList<PosPoseSnapshot>()

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("fakeplayer") {
            requires { it.isIngame }

            literal("spawn") {
                optional("name", ClientStringArgumentType.word(), default = "FakePlayer", suggestions) { name ->
                    exec { ctx ->
                        spawnCommand(ctx.get(name), false)
                    }
                }
            }
            literal("remove") {
                optional("name", ClientStringArgumentType.word(), default = "FakePlayer", suggestions) { name ->
                    exec { ctx ->
                        removeCommand(ctx.get(name))
                    }
                }
            }
            literal("clear") {
                exec {
                    clearCommand()
                }
            }
            literal("startrecording") {
                exec {
                    startRecordingCommand()
                }
            }
            literal("endrecording") {
                optional("name", ClientStringArgumentType.word(), default = "FakePlayer", suggestions) { name ->
                    exec { ctx ->
                        endRecordingCommand(ctx.get(name))
                    }
                }
            }
        }
    }

    private fun CmdI18n.spawnCommand(name: String, moving: Boolean): Int {
        checkInGame()
        spawn(name, moving)
        return 1
    }

    private fun CmdI18n.removeCommand(name: String): Int {
        checkInGame()

        if (fakePlayers.isEmpty()) {
            throw CommandException(t("noFakePlayers"))
        }

        val playersToRemove = fakePlayers.filterTo(ReferenceOpenHashSet()) {
            it.name.string.equals(name, ignoreCase = true)
        }

        if (playersToRemove.isEmpty()) {
            mc.gui.hud.chat.removeMessage("CFakePlayer#info")
            val data = MessageMetadata(id = "CFakePlayer#info", remove = false)

            chat(warning(t("remove.noFakePlayerNamed", name)), metadata = data)
            chat(regular(t("remove.currentlySpawned")), metadata = data)
            fakePlayers.forEach { fakePlayer ->
                chat(regular("- " + fakePlayer.name.string), metadata = data)
            }

            return 1
        }

        playersToRemove.forEach { fakePlayer ->
            world.removeEntity(fakePlayer.id, Entity.RemovalReason.KILLED)
            chat(
                regular(
                    t("remove.fakePlayerRemoved",
                        fakePlayer.x.roundToDecimalPlaces(),
                        fakePlayer.y.roundToDecimalPlaces(),
                        fakePlayer.z.roundToDecimalPlaces()
                    )
                ),
                metadata = MessageMetadata(id = "CFakePlayer#info")
            )
        }

        fakePlayers.removeAll(playersToRemove)
        return 1
    }

    private fun CmdI18n.clearCommand(): Int {
        checkInGame()

        if (fakePlayers.isEmpty()) {
            throw CommandException(t("noFakePlayers"))
        }

        fakePlayers.forEach { fakePlayer ->
            world.removeEntity(fakePlayer.id, Entity.RemovalReason.DISCARDED)
        }
        fakePlayers.clear()
        return 1
    }

    @Suppress("SpellCheckingInspection")
    private fun CmdI18n.startRecordingCommand(): Int {
        checkInGame()

        if (recording) {
            throw CommandException(t("startrecording.alreadyRecording"))
        }

        recording = true
        chat(
            regular(t("startrecording.startedRecording")),
            metadata = MessageMetadata(id = "CFakePlayer#info")
        )
        notification(
            "FakePlayer",
            t("startrecording.startedRecordingNotification"),
            NotificationEvent.Severity.INFO
        )
        return 1
    }

    @Suppress("SpellCheckingInspection")
    private fun CmdI18n.endRecordingCommand(name: String): Int {
        checkInGame()

        if (!recording) {
            throw CommandException(t("endrecording.notRecording"))
        }

        if (snapshots.isEmpty()) {
            throw CommandException(t("endrecording.somethingWentWrong"))
        }

        spawn(name, true)
        stopRecording()
        return 1
    }

    /**
     * Adds a new fake player.
     *
     * Note: a moving player requires [snapshots] to be not null.
     *
     * @param moving true if the fake player should play a recording.
     */
    private fun CmdI18n.spawn(nameArg: String, moving: Boolean) {
        val fakePlayer: FakePlayer

        if (moving) {
            fakePlayer = MovingFakePlayer(
                snapshots = snapshots.toTypedArray(),
                world,
                GameProfile(
                    UUID.randomUUID(),
                    nameArg
                ),
                fakePlayers::remove,
            )
        } else {
            fakePlayer = FakePlayer(
                world,
                GameProfile(
                    UUID.randomUUID(),
                    nameArg
                ),
                fakePlayers::remove,
            )
        }

        fakePlayer.id = world.nextLocalEntityId()

        if (!moving) {
            fakePlayer.loadAttributes(fromPlayer(player))
        }

        fakePlayers.add(fakePlayer)
        world.addEntity(fakePlayer)
        chat(
            regular(
                t("fakePlayerSpawned",
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
    private fun CmdI18n.checkInGame() {
        if (mc.level == null || mc.player == null) {
            throw CommandException(t("mustBeInGame"))
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
         * so an [ClientboundExplodePacket] handler is required.
         */
        if (packet is ClientboundExplodePacket) {
            fakePlayers.forEach { fakePlayer ->
                val damage = fakePlayer.getDamageFromExplosion(
                    pos = packet.center,
                    power = packet.radius
                )

                fakePlayer.applyEstimatedDamage(damage)
            }
        }

        /**
         * The server should not know that we tried to attack a fake player.
         */
        val interactEntityId = packet.entityIdC2SInteractOrAttack ?: return@handler
        if (fakePlayers.any { fakePlayer -> interactEntityId == fakePlayer.id }) {
            it.cancelEvent()
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (fakePlayers.isEmpty()) {
            return@handler
        }

        val contains = fakePlayers.any { player ->
            player.id == event.entity.id
        }

        if (!contains) {
            return@handler
        }

        val fakePlayer = event.entity as FakePlayer

        fakePlayer.applyEstimatedDamage(calculateAttackDamage(fakePlayer))
    }

    /**
     * Recordings are made in the tick event handler.
     */
    @Suppress("unused")
    val tickHandler = handler<GameTickEvent> {
        if (!recording) {
            return@handler
        }

        if (mc.level == null || mc.player == null) {
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

    private fun calculateAttackDamage(fakePlayer: LivingEntity): Float {
        var genericAttackDamage = if (player.isAutoSpinAttack) {
            player.autoSpinAttackDmg
        } else {
            player.getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat()
        }
        val damageSource = player.damageSources().playerAttack(player)
        var enchantAttackDamage =
            player.getEnchantedDamage(fakePlayer, genericAttackDamage, damageSource) - genericAttackDamage

        val attackCooldown = player.getAttackStrengthScale(0.5f)
        genericAttackDamage *= 0.2f + attackCooldown * attackCooldown * 0.8f
        enchantAttackDamage *= attackCooldown

        return fakePlayer.getEffectiveDamage(damageSource, genericAttackDamage + enchantAttackDamage, false)
    }

    private fun FakePlayer.applyEstimatedDamage(damage: Float) {
        if (damage <= 0f) {
            return
        }

        val absorbedDamage = damage.coerceAtMost(this.absorptionAmount)
        if (absorbedDamage > 0f) {
            this.absorptionAmount = (this.absorptionAmount - absorbedDamage).coerceAtLeast(0f)
        }

        val remainingDamage = damage - absorbedDamage
        if (remainingDamage > 0f) {
            this.health -= remainingDamage
        }
    }

}
