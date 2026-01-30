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
package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.fastutil.forEachInt
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot.isADuplicate
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.equipment.ArmorMaterials
import java.util.function.IntPredicate
import java.util.function.Predicate
import kotlin.math.abs

@Suppress("MagicNumber")
object CustomAntiBotMode : AntiBotMode("Custom") {

    private object InvalidGround : ToggleableValueGroup(ModuleAntiBot, "InvalidGround", true) {
        val vlToConsiderAsBot by int("VLToConsiderAsBot", 10, 1..50, "flags")
    }

    private val customConditions by multiEnumChoice<CustomConditions>(
        "Conditions",
        CustomConditions.NO_GAME_MODE,
        CustomConditions.ILLEGAL_PITCH,
        CustomConditions.FAKE_ENTITY_ID,
    )

    private object AlwaysInRadius : ToggleableValueGroup(ModuleAntiBot, "AlwaysInRadius", false) {
        val alwaysInRadiusRange by float("AlwaysInRadiusRange", 20f, 5f..30f)
    }

    private object Age : ToggleableValueGroup(ModuleAntiBot, "Age", false), AntiBotPredicate {
        private val minimum by int("Minimum", 20, 0..120, "ticks")

        override fun isBot(entity: Player): Boolean = entity.tickCount < minimum
    }

    private object Armor : ToggleableValueGroup(ModuleAntiBot, "Armor", false) {

        /**
         * @see ArmorMaterials
         */
        @Suppress("UNUSED")
        private enum class ArmorPredicate(
            override val tag: String,
            val predicate: Predicate<ItemStack>,
        ) : Tagged {
            // General
            NOTHING("Nothing", Predicate(ItemStack::isEmpty)),
            LEATHER(
                "Leather",
                Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
            ),
            CHAIN(
                "Chain",
                Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS
            ),
            IRON(
                "Iron",
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS
            ),
            GOLD(
                "Gold",
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS
            ),
            DIAMOND(
                "Diamond",
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS
            ),
            NETHERITE(
                "Netherite",
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS
            ),

            // Chestplate only
            ELYTRA("Elytra", Items.ELYTRA),

            // Helmet only
            TURTLE_SCUTE("TurtleScute", Items.TURTLE_HELMET),
            PUMPKIN("Pumpkin", Items.CARVED_PUMPKIN),
            SKULL("Skull", ItemTags.SKULLS);

            constructor(choiceName: String, tag: TagKey<Item>) : this(
                choiceName,
                Predicate { it.`is`(tag) }
            )

            constructor(choiceName: String, item: Item) : this(
                choiceName,
                Predicate { it.`is`(item) }
            )

            constructor(choiceName: String, vararg items: Item) : this(
                choiceName,
                Predicate { items.contains(it.item) }
            )
        }

        private val BASE = enumSetOf(
            ArmorPredicate.NOTHING, ArmorPredicate.LEATHER,
            ArmorPredicate.CHAIN, ArmorPredicate.IRON,
            ArmorPredicate.GOLD, ArmorPredicate.DIAMOND,
            ArmorPredicate.NETHERITE,
        )

        private val HELMET = enumSetOf(
            ArmorPredicate.NOTHING, ArmorPredicate.LEATHER,
            ArmorPredicate.CHAIN, ArmorPredicate.IRON,
            ArmorPredicate.GOLD, ArmorPredicate.DIAMOND,
            ArmorPredicate.NETHERITE, ArmorPredicate.TURTLE_SCUTE,
            ArmorPredicate.PUMPKIN, ArmorPredicate.SKULL,
        )

        private val CHESTPLATE = enumSetOf(
            ArmorPredicate.NOTHING, ArmorPredicate.LEATHER,
            ArmorPredicate.CHAIN, ArmorPredicate.IRON,
            ArmorPredicate.GOLD, ArmorPredicate.DIAMOND,
            ArmorPredicate.NETHERITE, ArmorPredicate.ELYTRA,
        )

        private val values = enumMapOf<EquipmentSlot, MultiChoiceListValue<ArmorPredicate>>(
            EquipmentSlot.HEAD, multiEnumChoice("Helmet", enumSetOf(ArmorPredicate.NOTHING), HELMET),
            EquipmentSlot.CHEST, multiEnumChoice("Chestplate", enumSetOf(ArmorPredicate.NOTHING), CHESTPLATE),
            EquipmentSlot.LEGS, multiEnumChoice("Leggings", enumSetOf(ArmorPredicate.NOTHING), BASE),
            EquipmentSlot.FEET, multiEnumChoice("Boots", enumSetOf(ArmorPredicate.NOTHING), BASE),
        )

        fun isValid(entity: Player): Boolean {
            return values.all { (slot, value) ->
                val predicates = value.get()
                val armor = entity.getItemBySlot(slot)
                // Nothing selected = skip this part
                predicates.isEmpty() || predicates.any {
                    it.predicate.test(armor)
                }
            }
        }
    }

    private object Name : ToggleableValueGroup(ModuleAntiBot, "Name", true), AntiBotPredicate {
        private val lengthRange by intRange("Length", 3..16, 1..32)
        private val validateChars by multiEnumChoice("ValidateChars", enumSetOf(CharacterValidator.VANILLA))

        /**
         * https://en.wikipedia.org/wiki/Unicode_block
         */
        private enum class CharacterValidator(override val tag: String) : Tagged, IntPredicate {
            VANILLA("Vanilla") {
                override fun test(value: Int): Boolean {
                    return value in '0'.code..'9'.code
                        || value in 'a'.code..'z'.code
                        || value in 'A'.code..'Z'.code
                        || value == '_'.code
                }
            },

            /** Cyrillic + Cyrillic Supplement */
            CYRILLIC("Cyrillic") {
                override fun test(value: Int): Boolean {
                    return value in 0x0400..0x052F
                }
            },

            CJK_UNIFIED_IDEOGRAPHS("CJKUnifiedIdeographs") {
                override fun test(value: Int): Boolean {
                    return value in 0x4E00..0x9FA5
                }
            };

            fun test(string: String): Boolean {
                return string.chars().allMatch(this)
            }
        }

        override fun isBot(entity: Player): Boolean {
            val name = entity.scoreboardName
            return name.length !in lengthRange || (validateChars.any { !it.test(name) })
        }
    }

    init {
        tree(InvalidGround)
        tree(AlwaysInRadius)
        tree(Age)
        tree(Armor)
        tree(Name)
    }

    private val flyingSet = Int2IntOpenHashMap()
    private val hitSet = IntOpenHashSet()
    private val notAlwaysInRadiusSet = IntOpenHashSet()

    private val swungSet = IntOpenHashSet()
    private val crittedSet = IntOpenHashSet()
    private val attributesSet = IntOpenHashSet()

    private val armorSet = IntOpenHashSet()

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = CRITICAL_MODIFICATION) {
        val rangeSquared = AlwaysInRadius.alwaysInRadiusRange.sq()
        for (entity in world.players()) {
            if (entity === player) {
                continue
            }

            if (player.distanceToSqr(entity) > rangeSquared) {
                notAlwaysInRadiusSet.add(entity.id)
            }

            if (Armor.enabled && !Armor.isValid(entity)) {
                armorSet.add(entity.id)
            }
        }

        armorSet.removeIf {
            val entity = world.getEntity(it) as? Player
            entity == null || Armor.isValid(entity)
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        hitSet.add(event.entity.id)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is ClientboundMoveEntityPacket -> {
                if (!packet.hasPosition() || !InvalidGround.enabled) {
                    return@handler
                }

                val entity = packet.getEntity(world) ?: return@handler
                val id = entity.id
                val currentValue = flyingSet.getOrDefault(id, 0)
                if (entity.onGround() && entity.yo != entity.y) {
                    flyingSet.put(id, currentValue + 1)
                } else if (!entity.onGround() && currentValue > 0) {
                    val newVL = currentValue / 2

                    if (newVL <= 0) {
                        flyingSet.remove(id)
                    } else {
                        flyingSet.put(id, newVL)
                    }
                }
            }

            is ClientboundUpdateAttributesPacket -> {
                attributesSet.add(packet.entityId)
            }

            is ClientboundAnimatePacket -> {
                when (packet.action) {
                    ClientboundAnimatePacket.SWING_MAIN_HAND, ClientboundAnimatePacket.SWING_OFF_HAND -> {
                        swungSet.add(packet.id)
                    }
                    ClientboundAnimatePacket.CRITICAL_HIT, ClientboundAnimatePacket.MAGIC_CRITICAL_HIT -> {
                        crittedSet.add(packet.id)
                    }
                }
            }

            is ClientboundRemoveEntitiesPacket -> {
                packet.entityIds.forEachInt { entityId ->
                    attributesSet.remove(entityId)
                    flyingSet.remove(entityId)
                    hitSet.remove(entityId)
                    notAlwaysInRadiusSet.remove(entityId)
                    armorSet.remove(entityId)
                }
            }
        }
    }

    private fun hasInvalidGround(player: Player): Boolean {
        return flyingSet.getOrDefault(player.id, 0) >= InvalidGround.vlToConsiderAsBot
    }

    override fun isBot(entity: Player): Boolean {
        val entityId = entity.id
        return when {
            InvalidGround.enabled && hasInvalidGround(entity) -> true
            AlwaysInRadius.enabled && !notAlwaysInRadiusSet.contains(entityId) -> true
            Age.enabled && Age.isBot(entity) -> true
            Armor.enabled && armorSet.contains(entityId) -> true
            Name.enabled && Name.isBot(entity) -> true
            else -> customConditions.any { it.isBot(entity) }
        }
    }

    override fun reset() {
        flyingSet.clear()
        notAlwaysInRadiusSet.clear()
        hitSet.clear()
        swungSet.clear()
        crittedSet.clear()
        attributesSet.clear()
        armorSet.clear()
    }

    @Suppress("unused")
    private enum class CustomConditions(
        override val tag: String,
        private val isBot: AntiBotPredicate
    ) : Tagged, AntiBotPredicate by isBot {
        DUPLICATE("Duplicate", { suspected ->
            isADuplicate(suspected.gameProfile)
        }),
        NO_GAME_MODE("NoGameMode", { suspected ->
            network.getPlayerInfo(suspected.uuid)?.gameMode == null
        }),
        ILLEGAL_PITCH("IllegalPitch", { suspected ->
            abs(suspected.xRot) > 90
        }),
        FAKE_ENTITY_ID("FakeEntityID", { suspected ->
            suspected.id !in 0..1_000_000_000
        }),
        NEED_IT("NeedHit", { suspected ->
            !hitSet.contains(suspected.id)
        }),
        ILLEGAL_HEALTH("IllegalHealth", { suspected ->
            suspected.health > player.maxHealth
        }),
        SWUNG("Swung", { suspected ->
            !swungSet.contains(suspected.id)
        }),
        CRITTED("Critted", { suspected ->
            !crittedSet.contains(suspected.id)
        }),
        ATTRIBUTES("Attributes", { suspected ->
            !attributesSet.contains(suspected.id)
        }),
        ILLEGAL_SCALE("IllegalScale", { suspected ->
            suspected.attributes.getValue(Attributes.SCALE) != 1.0
        })
    }
}
