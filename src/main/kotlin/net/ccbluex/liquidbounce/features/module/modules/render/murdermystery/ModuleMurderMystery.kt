package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.BoxRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.item.BowItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket
import net.minecraft.sound.SoundEvent
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box

object ModuleMurderMystery : ClientModule("MurderMystery", Category.RENDER) {
    var playHurt = false
    var playBow = false

    val modes =
        choices(
            "Mode",
            MurderMysteryClassicMode,
            arrayOf(MurderMysteryClassicMode, MurderMysteryInfectionMode, MurderMysteryAssassinationMode),
        )

    private val currentMode: MurderMysteryMode
        get() = this.modes.activeChoice as MurderMysteryMode

    override fun onDisabled() {
        this.reset()
    }

    private fun reset() {
        this.currentMode.reset()
    }

    @Suppress("unused")
    val worldChangeEventHandler = handler<WorldChangeEvent> {
        this.reset()
    }

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> { event ->
        if (playHurt) {
            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvent.of(Identifier.of("entity.villager.hurt")),
                    1F,
                ),
            )

            playHurt = false
        }

        if (playBow) {
            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvent.of(Identifier.of("item.crossbow.shoot")),
                    1F,
                ),
            )

            playBow = false
        }

        world.entities.filterIsInstance<ArmorStandEntity>().forEach {
            if (it.getEquippedStack(EquipmentSlot.MAINHAND).item is BowItem && it.isInvisible) {
                renderDroppedBowBox(event, it)
            }
        }

    }

    val packetHandler = handler<PacketEvent> { packetEvent ->
        val world = mc.world ?: return@handler

        if (packetEvent.packet is EntityEquipmentUpdateS2CPacket) {
            val packet: EntityEquipmentUpdateS2CPacket = packetEvent.packet

            packet.equipmentList
                .filter {
                    !it.second.isEmpty && it.first in
                        arrayOf(
                            EquipmentSlot.MAINHAND,
                            EquipmentSlot.OFFHAND,
                        )
                }
                .forEach {
                    val itemStack = it.second
                    val entity = world.getEntityById(packet.entityId)

                    handleItem(itemStack, entity)
                }
        }
        if (packetEvent.packet is GameJoinS2CPacket || packetEvent.packet is PlayerRespawnS2CPacket) {
            this.reset()
        }
    }

    @Suppress("unused")
    val tagHandler = handler<TagEntityEvent> {
        if (it.entity !is AbstractClientPlayerEntity) {
            return@handler
        }

        if (!shouldAttack(it.entity)) {
            it.dontTarget()
        }

        val playerType = this.currentMode.getPlayerType(it.entity)
        val entity = it.entity

        val col = when (playerType) {
            MurderMysteryMode.PlayerType.DETECTIVE_LIKE -> {
                entity.scoreboard.getTeam(entity.gameProfile.name)?.prefix = Text.literal("§b[BOW] ")
                Color4b(0, 144, 255)
            }

            MurderMysteryMode.PlayerType.MURDERER -> {
                entity.scoreboard.getTeam(entity.gameProfile.name)?.prefix = Text.literal("§c[MURD] ")
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
        if (entity !is AbstractClientPlayerEntity) {
            return
        }

        val isSword = MurderMysterySwordDetection.isSword(itemStack)
        val isBow = itemStack.item is BowItem

        val locationSkin = entity.skinTextures.texture

        when {
            isSword -> currentMode.handleHasSword(entity, locationSkin)
            isBow -> currentMode.handleHasBow(entity, locationSkin)
        }
    }

    private fun renderDroppedBowBox(event: WorldRenderEvent, armorStandEntity: ArmorStandEntity) {
        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            BoxRenderer.drawWith(this) {
                val box = Box(-0.6, 0.0, -0.6, 0.6, 2.5, 0.6)
                val pos = armorStandEntity.interpolateCurrentPosition(event.partialTicks)

                withPositionRelativeToCamera(pos) {
                    drawBox(
                        box,
                        Color4b(127, 255, 212, 100), Color4b(0, 255, 255)
                    )
                }
            }
        }
    }

    private fun shouldAttack(entityPlayer: AbstractClientPlayerEntity): Boolean {
        return this.currentMode.shouldAttack(entityPlayer)
    }

    fun disallowsArrowDodge(): Boolean {
        if (!running) {
            return false
        }

        return this.currentMode.disallowsArrowDodge()
    }
}
