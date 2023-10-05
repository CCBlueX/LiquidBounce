/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.math.levenshtein
import net.minecraft.block.Blocks
import net.minecraft.block.MapColor
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.*
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import java.awt.Color
import java.util.*
import kotlin.math.absoluteValue

object ModuleMurderMystery : Module("MurderMystery", Category.RENDER) {
    private val bowSkins = HashSet<String>()
    private val murdererSkins = HashSet<String>()

    private var playHurt = false
    private var playBow = false
    private var currentAssasinationTarget: UUID? = null
    private var currentAssasin: UUID? = null

    private var lastMap: String? = null

    private val modes = choices("Mode", ClassicMode, arrayOf(ClassicMode, InfectionMode, AssassinationMode))

    private object ClassicMode : Choice("Classic") {
        override val parent: ChoiceConfigurable
            get() = modes
    }

    private object InfectionMode : Choice("Infection") {
        override val parent: ChoiceConfigurable
            get() = modes
    }

    private object AssassinationMode : Choice("Assassination") {
        override val parent: ChoiceConfigurable
            get() = modes
    }

    override fun disable() {
        this.bowSkins.clear()
        this.murdererSkins.clear()
    }

    private val gameRenderEvent = handler<WorldRenderEvent> {
        if (playHurt) {
            mc.soundManager.play(PositionedSoundInstance.master(SoundEvent.of(Identifier("entity.villager.hurt")), 1F))

            playHurt = false
        }

        if (playBow) {
            mc.soundManager.play(PositionedSoundInstance.master(SoundEvent.of(Identifier("item.crossbow.shoot")), 1F))

            playBow = false
        }
    }

    val motionUpdateHandler = handler<GameTickEvent> { event ->
        val world = mc.world ?: return@handler
        val player = mc.player ?: return@handler

        if (this.modes.activeChoice === InfectionMode) {
            world.players
                .filterIsInstance<AbstractClientPlayerEntity>()
                .filter {
                    it.isUsingItem && arrayOf(it.mainHandStack, it.offHandStack).any { stack -> stack.item is BowItem }
                }
                .forEach { playerEntity ->
                    handleHasBow(playerEntity, playerEntity.skinTexture, isCharging=true)
                }
        }

        if (this.modes.activeChoice !== AssassinationMode) {
            return@handler
        }

        val equippedItem = player.inventory.getStack(3)

        val item = equippedItem?.item

        if (item !is FilledMapItem) {
            return@handler
        }

        val mapName = FilledMapItem.getMapId(equippedItem)?.let { FilledMapItem.getMapName(it) }
        val mapData = mapName?.let { world.getMapState(it) } ?: return@handler


        if (
//            equippedItem.name.string != "§cKill Contract"
            mapName == this.lastMap
        ) {
            return@handler
        }

        println(mapName + "/" + equippedItem.name.string)
        lastMap = mapName

        val rgb = IntArray(128 * 128)

        (0 until rgb.size).forEach { i ->

//            if (j / 4 == 0) {
//                rgb[i] = (i + ((i / 128) and 1)) * 8 + (16 shl 24)
//            } else {
            val color = MapColor.getRenderColor(mapData.colors[i].toInt())

            val r = color and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = (color ushr 16) and 0xFF

            rgb[i] = Color(r, g, b).getRGB()
//            }

        }

        val contractLine = IntArray(128 * 7)

        (0 until 7).forEach { y ->
            (0 until 128).forEach { x ->
                var newRGB = rgb[128 * 105 + y * 128 + x]

                newRGB = if (newRGB == Color(123, 102, 62).rgb || newRGB == Color(143, 119, 72).rgb) {
                    0
                } else {
                    -1
                }

                contractLine[128 * y + x] = newRGB
            }
        }

        val output = StringBuilder()

        var lastNonEmptyScanline = -1
        var emptyScanlines = 0

        (0 until 128).forEach { x ->
            var isEmpty = true

            for (y in 0 until 7) {
                if (contractLine[128 * y + x] == -1) {
                    isEmpty = false
                    break
                }
            }

            if (isEmpty) {
                if (emptyScanlines++ > 3) {
                    output.append(" ")
                    emptyScanlines = 0
                }
            }

            if (lastNonEmptyScanline != -1 && isEmpty) {
                var yoff = lastNonEmptyScanline
                var off: Int

                val w = x - lastNonEmptyScanline
                val h = 7

                val fingerPrint = BooleanArray(w * h)

                var y1 = 0

                while (y1 < h) {
                    off = yoff

                    for (x1 in 0 until w) {
                        fingerPrint[y1 * w + x1] = contractLine[off++] == -1
                    }

                    y1++
                    yoff += 128
                }

                var letter: String? = null

                for ((key, value1) in ModuleMurderMystery.FontRecognition.LETTER_MAP.entries) {
                    if (value1.contentEquals(fingerPrint)) {
                        letter = key
                        break
                    }
                }

                output.append(letter ?: "?")

                lastNonEmptyScanline = -1
            }

            if (!isEmpty && lastNonEmptyScanline == -1) {
                lastNonEmptyScanline = x
                emptyScanlines = 0
            }
        }

        val s = output.toString().trim { it <= ' ' }.split(" ").toTypedArray()

        println(s.contentToString())

        if (s.isNotEmpty() && s[0].startsWith("NAME:")) {
            val target = s[0].substring("NAME:".length).lowercase(Locale.getDefault()).trim()

            val targetPlayer = player.networkHandler.playerList.minByOrNull { netInfo ->
                levenshtein(target, netInfo.profile.name.lowercase().trim())
            }

            if (targetPlayer != null) {
                currentAssasinationTarget = targetPlayer.profile.id

                chat("Target: " + targetPlayer.profile.name)
            } else {
                chat("Failed to find target, but the name is: $target")
            }
        }
    }

    val packetHandler = handler<PacketEvent> { packetEvent ->
        val world = mc.world ?: return@handler

        if (packetEvent.packet is PlaySoundS2CPacket) {
            val packet = packetEvent.packet

            if (packet.sound.value().id.toString() == "minecraft:block.note_block.basedrum") {
//                println("${packet.sound.value().id}/${packet.volume}")

                // Fitted by observed values
                val expectedDistance = ((1/ packet.volume) - 0.98272992) / 0.04342088

                val probablyAssassin = world.players.minByOrNull {
                    (it.distanceTo(player) - expectedDistance).absoluteValue
                } ?: return@handler

                val newAssasin = probablyAssassin.gameProfile.id

                if (currentAssasin != newAssasin)
                    chat("Your Assassin: " + probablyAssassin.gameProfile.name)

                currentAssasin = newAssasin
            }
        }

        if (packetEvent.packet is EntityEquipmentUpdateS2CPacket) {
            val packet: EntityEquipmentUpdateS2CPacket = packetEvent.packet

            packet.equipmentList
                .filter {
                    !it.second.isEmpty && it.first in arrayOf(
                        EquipmentSlot.MAINHAND,
                        EquipmentSlot.OFFHAND
                    )
                }
                .forEach {
                    val itemStack = it.second
                    val item = itemStack.item
                    val entity = world.getEntityById(packet.id)

                    handleItem(item, entity)
                }

        }
        if (packetEvent.packet is GameJoinS2CPacket || packetEvent.packet is PlayerRespawnS2CPacket) {
            murdererSkins.clear()
            bowSkins.clear()

            currentAssasinationTarget = null
            currentAssasin = null
        }
    }

    private fun handleItem(item: Item?, entity: Entity?) {
        val isSword = isSword(item)

        val isBow = item is BowItem

        if (entity !is AbstractClientPlayerEntity) {
            return
        }

        val locationSkin = entity.skinTexture

        when {
            isSword -> handleHasSword(entity, locationSkin)
            isBow -> handleHasBow(entity, locationSkin, isCharging = false)
        }
    }

    private fun handleHasBow(entity: AbstractClientPlayerEntity, locationSkin: Identifier, isCharging: Boolean) {
        if (this.modes.activeChoice === AssassinationMode) {
            return
        }
        if (this.modes.activeChoice === InfectionMode && !isCharging) {
            return
        }

        if (bowSkins.add(locationSkin.path)) {
            chat(entity.gameProfile.name + " has a bow.")

            playBow = true
        }
    }

    private fun handleHasSword(entity: AbstractClientPlayerEntity, locationSkin: Identifier) {
        when (modes.activeChoice) {
            AssassinationMode -> {}
            InfectionMode -> {
                if (murdererSkins.add(locationSkin.path) && murdererSkins.size == 1) {
                    chat(entity.gameProfile.name + " is infected.")
                }
            }

            ClassicMode -> {
                if (murdererSkins.add(locationSkin.path)) {
                    chat("It's " + entity.gameProfile.name)

                    playHurt = true
                }
            }

            else -> {}
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun isSword(item: Item?): Boolean {
        return item is SwordItem ||
            item is PickaxeItem ||
            item is ShovelItem && item != Items.WOODEN_SHOVEL && item != Items.GOLDEN_SHOVEL ||
            item is AxeItem ||
            item is HoeItem ||
            item is BoatItem ||
            run {
                if (item !is BlockItem) {
                    return@run false
                }

                val block = item.block

                return@run block == Blocks.SPONGE ||
                    block == Blocks.DEAD_BUSH ||
                    block == Blocks.REDSTONE_TORCH ||
                    block == Blocks.CHORUS_PLANT
            } ||
            item in arrayOf(
            Items.GOLDEN_CARROT,
            Items.CARROT,
            Items.CARROT_ON_A_STICK,
            Items.BONE,
            Items.TROPICAL_FISH,
            Items.PUFFERFISH,
            Items.SALMON,
            Items.BLAZE_ROD,
            Items.PUMPKIN_PIE,
            Items.NAME_TAG,
            Items.APPLE,
            Items.FEATHER,
            Items.COOKIE,
            Items.SHEARS,
            Items.COOKED_SALMON,
            Items.STICK,
            Items.QUARTZ,
            Items.ROSE_BUSH,
            Items.ICE,
            Items.COOKED_BEEF,
            Items.NETHER_BRICK,
            Items.COOKED_CHICKEN,
            Items.MUSIC_DISC_BLOCKS,
            Items.RED_DYE,
            Items.OAK_BOAT,
            Items.BOOK,
            Items.GLISTERING_MELON_SLICE
        )
    }

    fun getColor(entityPlayer: Entity): Color4b? {
        if (!enabled || entityPlayer !is AbstractClientPlayerEntity) {
            return null
        }

        if (modes.activeChoice === AssassinationMode) {
            if (entityPlayer.gameProfile.id == currentAssasinationTarget || entityPlayer.gameProfile.id == currentAssasin) {
                return Color4b(203, 9, 9)
            }
        } else {
            if (isMurderer(entityPlayer)) {
                return Color4b(203, 9, 9)
            } else if (bowSkins.contains(entityPlayer.skinTexture.path)) {
                return Color4b(0, 144, 255)
            }
        }

        return null
    }

    fun isMurderer(entityPlayer: AbstractClientPlayerEntity) = murdererSkins.contains(entityPlayer.skinTexture.path)
    fun disallowsArrowDodge(): Boolean {
        if (!enabled) {
            return false
        }

        if (this.modes.activeChoice === InfectionMode) {
            // Don't dodge if we are not dead yet.
            return player.handItems.any { it.item is BowItem || it.item == Items.ARROW }
        }

        return false
    }

    object FontRecognition {
        val LETTER_MAP = HashMap<String, BooleanArray>()

        init {
            LETTER_MAP["A"] = booleanArrayOf(
                false,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true
            )
            LETTER_MAP["C"] = booleanArrayOf(
                false,
                true,
                true,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                true,
                true
            )
            LETTER_MAP["D"] = booleanArrayOf(
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                false
            )
            LETTER_MAP[":"] = booleanArrayOf(false, false, false, true, false, false, true)
            LETTER_MAP["E"] = booleanArrayOf(
                true,
                true,
                true,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                true,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                true,
                true
            )
            LETTER_MAP["B"] = booleanArrayOf(
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                false
            )
            LETTER_MAP["H"] = booleanArrayOf(
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true
            )
            LETTER_MAP["I"] = booleanArrayOf(true, true, true, true, true, true, true)
            LETTER_MAP["J"] =
                booleanArrayOf(true, true, false, true, false, true, false, true, false, true, false, true, true, false)
            LETTER_MAP["K"] = booleanArrayOf(
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true
            )
            LETTER_MAP["L"] = booleanArrayOf(
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                true,
                true
            )
            LETTER_MAP["M"] = booleanArrayOf(
                true,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                true
            )
            LETTER_MAP["N"] = booleanArrayOf(
                true,
                false,
                false,
                true,
                true,
                false,
                false,
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                false,
                false,
                true,
                true,
                false,
                false,
                true,
                true,
                false,
                false,
                true
            )
            LETTER_MAP["O"] = booleanArrayOf(
                false,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                false,
                true,
                false
            )
            LETTER_MAP["R"] = booleanArrayOf(
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true
            )
            LETTER_MAP["S"] = booleanArrayOf(
                false,
                true,
                true,
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                true,
                false,
                false,
                true,
                true,
                true,
                false
            )
            LETTER_MAP["T"] = booleanArrayOf(
                true,
                true,
                true,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false
            )
            LETTER_MAP["U"] = booleanArrayOf(
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                false,
                true,
                false
            )
            LETTER_MAP["X"] = booleanArrayOf(
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true
            )
            LETTER_MAP["Y"] = booleanArrayOf(
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                true,
                false
            )
        }
    }

}
