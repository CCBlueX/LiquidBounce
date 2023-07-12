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
import net.ccbluex.liquidbounce.utils.math.Levenshtein
import net.minecraft.block.Blocks
import net.minecraft.block.MapColor
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.*
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import java.awt.Color
import java.util.*

object ModuleMurderMystery : Module("MurderMystery", Category.RENDER) {
    private val bowSkins = HashSet<String>()
    private val murdererSkins = HashSet<String>()

    private var playHurt = false
    private var playBow = false
    private var currentAssasinationTarget: UUID? = null

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

    private val motionUpdateHandler = handler<GameTickEvent> { event ->
        val world = mc.world ?: return@handler
        val player = mc.player ?: return@handler

        if (this.modes.activeChoice === InfectionMode) {
            world.players.filter { it.isUsingItem }.filterIsInstance<AbstractClientPlayerEntity>()
                .forEach { playerEntity ->
                    val hasBow = arrayOf(playerEntity.mainHandStack, playerEntity.offHandStack)
                        .any { it.item is BowItem }

                    if (hasBow) {
                        this.bowSkins += playerEntity.skinTexture.path
                    }
                }

//            for (EntityPlayer playerEntity : mc.theWorld.playerEntities) {
//                if (playerEntity.getHeldItem() != null && playerEntity.getHeldItem().getItem() instanceof ItemBow && playerEntity.isUsingItem()) {
//                    if (!(playerEntity instanceof AbstractClientPlayer))
//                        continue;
//
//                    this.bowSkins.add(((AbstractClientPlayer) playerEntity).getLocationSkin().getResourcePath());
//                }
//            }
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

        if (equippedItem.name.string != "§cKill Contract" || mapName == this.lastMap) {
            return@handler
        }

        val rgb = IntArray(128 * 128)

        (0 until rgb.size).forEach { i ->
            val j = mapData.colors[i].toInt() and 0xFF

            if (j / 4 == 0) {
                rgb[i] = (i + ((i / 128) and 1)) * 8 + (16 shl 24)
            } else {
                rgb[i] = MapColor.get(j / 4).getRenderColor(MapColor.Brightness.validateAndGet(j and 3))
            }

        }

        val contractLine = IntArray(128 * 7)

        (0 until 7).forEach { y ->
            (0..128).forEach { x ->
                var newRGB = rgb[128 * 105 + y * 128 + x]

                newRGB = if (newRGB == Color(123, 102, 62).getRGB() || newRGB == Color(143, 119, 72).getRGB()) {
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

                for ((key, value1) in FontRecognition.LETTER_MAP.entries) {
                    if (value1.contentEquals(fingerPrint)) {
                        letter = key
                        break
                    }
                }

                output.append(letter ?: "?")

//                        BufferedImage bufferedImage = new BufferedImage(x - lastNonEmptyScanline, 7, BufferedImage.TYPE_INT_RGB);
//
//                        bufferedImage.setRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), contractLine, lastNonEmptyScanline, 128);
// //                        bufferedImage.setRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), letter, 0, 0);
//
//                        int hash = 1;
//
//                        for (int x1 = 0; x1 < bufferedImage.getWidth(); x1++) {
//                            for (int y2 = 0; y2 < bufferedImage.getHeight(); y2++) {
//                                hash = 31 * hash + bufferedImage.getRGB(x1, y2);
//                            }
//                        }
//
//                        try {
//                            ImageIO.write(bufferedImage, "PNG", new File("A:/letters/" + Integer.toHexString(hash) + ".png"));
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }

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
            val target = s[0].substring("NAME:".length).lowercase(Locale.getDefault())

            val targetPlayer = player.networkHandler.playerList.minByOrNull { netInfo ->
                Levenshtein.distance(
                    target,
                    netInfo.profile.name.lowercase()
                )
            }

            if (targetPlayer != null) {
                currentAssasinationTarget = targetPlayer.profile.id

                chat("Target: " + targetPlayer.profile.name)
            }
        }

        lastMap = mapName
    }

    private val packetHandler = handler<PacketEvent> { packetEvent ->
        val world = mc.world ?: return@handler

        if (packetEvent.packet is EntityEquipmentUpdateS2CPacket) {
            val packet = packetEvent.packet

            packet.equipmentList.filter {
                !it.second.isEmpty && it.first in arrayOf(
                    EquipmentSlot.MAINHAND,
                    EquipmentSlot.OFFHAND
                )
            }.forEach {
                val itemStack = it.second
                val item = itemStack.item

                val isSword =
                    item is SwordItem ||
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
                        Items.STICK
                    )

                val isBow = item is BowItem

                if (!isSword && !isBow) {
                    return@forEach
                }

                val entity = world.getEntityById(packet.id)

                if (entity !is AbstractClientPlayerEntity) {
                    return@forEach
                }

                val locationSkin = entity.skinTexture

                if (isSword && modes.activeChoice !== AssassinationMode) {
                    if (murdererSkins.add(locationSkin.path)) {
                        if (modes.activeChoice === InfectionMode) {
                            if (murdererSkins.size == 1) {
                                chat("Alpha: " + entity.gameProfile.name)
                            }
                        } else {
                            chat("It's " + entity.gameProfile.name)

                            playHurt = true
                        }
                    }
                } else if (modes.activeChoice === ClassicMode) {
                    if (bowSkins.add(locationSkin.path)) {
                        chat(entity.gameProfile.name + " has a bow.")

                        playBow = true
                    }
                }
            }

        }
        if (packetEvent.packet is GameJoinS2CPacket || packetEvent.packet is PlayerRespawnS2CPacket) {
            murdererSkins.clear()
            bowSkins.clear()

            currentAssasinationTarget = null
        }
    }

    fun getColor(entityPlayer: Entity): Color4b? {
        if (!enabled || entityPlayer !is AbstractClientPlayerEntity) {
            return null
        }

        if (modes.activeChoice === AssassinationMode) {
            if (currentAssasinationTarget != null && entityPlayer.gameProfile.id.equals(currentAssasinationTarget)) {
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
