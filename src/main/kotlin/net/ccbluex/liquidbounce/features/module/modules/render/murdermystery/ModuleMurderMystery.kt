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
package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.fastutil.forEachIsInstance
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.ChatFormatting
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB

object ModuleMurderMystery : ClientModule("MurderMystery", ModuleCategories.RENDER) {
    var playHurt = false
    var playBow = false

    val modes =
        choices(
            "Mode",
            MurderMysteryClassicMode,
            arrayOf(MurderMysteryClassicMode, MurderMysteryInfectionMode, MurderMysteryAssassinationMode),
        )

    private val currentMode: MurderMysteryMode
        get() = this.modes.activeMode

    override fun onDisabled() {
        this.reset()
    }

    private fun reset() {
        this.currentMode.reset()
    }

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> { event ->
        if (playHurt) {
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_HURT, 1F))

            playHurt = false
        }

        if (playBow) {
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.CROSSBOW_SHOOT, 1F))

            playBow = false
        }

        renderEnvironmentForWorld(event.matrixStack) {
            startBatch()
            world.entitiesForRendering().forEachIsInstance<ArmorStand> {
                if (it.getItemBySlot(EquipmentSlot.MAINHAND).item is BowItem && it.isInvisible) {
                    renderDroppedBowBox(event.partialTicks, it)
                }
            }
            commitBatch()
        }
    }

    val packetHandler = handler<PacketEvent> { packetEvent ->
        val world = mc.level ?: return@handler

        if (packetEvent.packet is ClientboundSetEquipmentPacket) {
            val packet: ClientboundSetEquipmentPacket = packetEvent.packet

            packet.slots
                .filter {
                    !it.second.isEmpty && it.first.type == EquipmentSlot.Type.HAND
                }
                .forEach {
                    val itemStack = it.second
                    val entity = world.getEntity(packet.entity)

                    handleItem(itemStack, entity)
                }
        }
        if (packetEvent.packet is ClientboundLoginPacket || packetEvent.packet is ClientboundRespawnPacket) {
            this.reset()
        }
    }

    val tagHandler = handler<TagEntityEvent> {
        if (it.entity !is AbstractClientPlayer) {
            return@handler
        }

        if (!shouldAttack(it.entity)) {
            it.dontTarget()
        }

        val playerType = this.currentMode.getPlayerType(it.entity)
        val entity = it.entity

        val col = when (playerType) {
            MurderMysteryMode.PlayerType.DETECTIVE_LIKE -> {
                entity.team?.setPlayerPrefix("[BOW] ".asPlainText(ChatFormatting.AQUA))
                Color4b(0, 144, 255)
            }

            MurderMysteryMode.PlayerType.MURDERER -> {
                entity.team?.setPlayerPrefix("[MURD] ".asPlainText(ChatFormatting.RED))
                Color4b(203, 9, 9)
            }

            MurderMysteryMode.PlayerType.NEUTRAL -> return@handler
        }

        it.color(col, Priority.IMPORTANT_FOR_USAGE_3)
    }

    private fun handleItem(
        itemStack: ItemStack,
        entity: Entity?,
    ) {
        if (entity !is AbstractClientPlayer) {
            return
        }

        val isSword = MurderMysterySwordDetection.isSword(itemStack)
        val isBow = itemStack.item is BowItem

        val locationSkin = entity.skin.body.texturePath()

        when {
            isSword -> currentMode.handleHasSword(entity, locationSkin)
            isBow -> currentMode.handleHasBow(entity, locationSkin)
        }
    }

    private fun WorldRenderEnvironment.renderDroppedBowBox(partialTicks: Float, armorStandEntity: ArmorStand) {
        val box = AABB(-0.6, 0.0, -0.6, 0.6, 2.5, 0.6)
        val pos = armorStandEntity.interpolateCurrentPosition(partialTicks)

        withPositionRelativeToCamera(pos) {
            drawBox(
                box,
                Color4b(127, 255, 212, 100), Color4b(0, 255, 255)
            )
        }
    }

    private fun shouldAttack(entityPlayer: AbstractClientPlayer): Boolean {
        return this.currentMode.shouldAttack(entityPlayer)
    }

    fun disallowsArrowDodge(): Boolean {
        if (!running) {
            return false
        }

        return this.currentMode.disallowsArrowDodge()
    }
}
