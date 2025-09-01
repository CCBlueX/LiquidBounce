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
package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot.isADuplicate
import net.ccbluex.liquidbounce.utils.item.material
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.block.AbstractSkullBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.item.equipment.ArmorMaterial
import net.minecraft.item.equipment.ArmorMaterials
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import java.util.*
import java.util.function.Predicate
import kotlin.math.abs

@Suppress("MagicNumber")
object CustomAntiBotMode : Choice("Custom"), ModuleAntiBot.IAntiBotMode {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiBot.modes

    private object InvalidGround : ToggleableConfigurable(ModuleAntiBot, "InvalidGround", true) {
        val vlToConsiderAsBot by int("VLToConsiderAsBot", 10, 1..50, "flags")
    }

    private val customConditions by multiEnumChoice<CustomConditions>(
        "Conditions",
        CustomConditions.NO_GAME_MODE,
        CustomConditions.ILLEGAL_PITCH,
        CustomConditions.FAKE_ENTITY_ID,
        CustomConditions.ILLEGAL_NAME,
    )

    private object AlwaysInRadius : ToggleableConfigurable(ModuleAntiBot, "AlwaysInRadius", false) {
        val alwaysInRadiusRange by float("AlwaysInRadiusRange", 20f, 5f..30f)
    }

    private object Age : ToggleableConfigurable(ModuleAntiBot, "Age", false) {
        val minimum by int("Minimum", 20, 0..120, "ticks")
    }

    private object Armor : ToggleableConfigurable(ModuleAntiBot, "Armor", false) {
        @Suppress("UNUSED")
        private enum class ArmorPredicate(
            override val choiceName: String,
            val predicate: Predicate<ItemStack>,
        ) : NamedChoice {
            // General
            NOTHING("Nothing", ItemStack::isEmpty),
            LEATHER("Leather", ArmorMaterials.LEATHER),
            CHAIN("Chain", ArmorMaterials.CHAIN),
            IRON("Iron", ArmorMaterials.IRON),
            GOLD("Gold", ArmorMaterials.GOLD),
            DIAMOND("Diamond", ArmorMaterials.DIAMOND),
            NETHERITE("Netherite", ArmorMaterials.NETHERITE),

            // Chestplate only
            ELYTRA("Elytra", Items.ELYTRA),

            // Helmet only
            TURTLE_SCUTE("TurtleScute", ArmorMaterials.TURTLE_SCUTE),
            PUMPKIN("Pumpkin", Items.CARVED_PUMPKIN),
            SKULL("Skull", { (it.item as? BlockItem)?.block is AbstractSkullBlock });

            constructor(choiceName: String, material: ArmorMaterial) : this(
                choiceName,
                { (it.item as? ArmorItem)?.material() == material }
            )

            constructor(choiceName: String, item: Item) : this(choiceName, { it.item === item })
        }

        private val BASE = EnumSet.of(
            ArmorPredicate.NOTHING, ArmorPredicate.LEATHER,
            ArmorPredicate.CHAIN, ArmorPredicate.IRON,
            ArmorPredicate.GOLD, ArmorPredicate.DIAMOND,
            ArmorPredicate.NETHERITE,
        )

        private val HELMET = EnumSet.of(
            ArmorPredicate.NOTHING, ArmorPredicate.LEATHER,
            ArmorPredicate.CHAIN, ArmorPredicate.IRON,
            ArmorPredicate.GOLD, ArmorPredicate.DIAMOND,
            ArmorPredicate.NETHERITE, ArmorPredicate.TURTLE_SCUTE,
            ArmorPredicate.PUMPKIN, ArmorPredicate.SKULL,
        )

        private val CHESTPLATE = EnumSet.of(
            ArmorPredicate.NOTHING, ArmorPredicate.LEATHER,
            ArmorPredicate.CHAIN, ArmorPredicate.IRON,
            ArmorPredicate.GOLD, ArmorPredicate.DIAMOND,
            ArmorPredicate.NETHERITE, ArmorPredicate.ELYTRA,
        )

        private val values = arrayOf(
            multiEnumChoice("Helmet", EnumSet.of(ArmorPredicate.NOTHING), HELMET),
            multiEnumChoice("Chestplate", EnumSet.of(ArmorPredicate.NOTHING), CHESTPLATE),
            multiEnumChoice("Leggings", EnumSet.of(ArmorPredicate.NOTHING), BASE),
            multiEnumChoice("Boots", EnumSet.of(ArmorPredicate.NOTHING), BASE),
        )

        fun isValid(entity: PlayerEntity): Boolean {
            return entity.armorItems.withIndex().all { (index, armor) ->
                val predicates = values[values.lastIndex - index].get()
                // Nothing selected = skip this part
                return predicates.isEmpty() || predicates.any {
                    it.predicate.test(armor)
                }
            }
        }
    }

    init {
        tree(InvalidGround)
        tree(AlwaysInRadius)
        tree(Age)
        tree(Armor)
    }

    private val flyingSet = Int2IntOpenHashMap()
    private val hitSet = IntOpenHashSet()
    private val notAlwaysInRadiusSet = IntOpenHashSet()

    private val swungSet = IntOpenHashSet()
    private val crittedSet = IntOpenHashSet()
    private val attributesSet = IntOpenHashSet()

    private val armorSet = IntOpenHashSet()

    private inline fun IntOpenHashSet.filterInPlace(predicate: (Int) -> Boolean) {
        val iter = this.intIterator()
        while (iter.hasNext()) {
            if (predicate(iter.nextInt())) {
                iter.remove()
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = CRITICAL_MODIFICATION) {
        val rangeSquared = AlwaysInRadius.alwaysInRadiusRange.sq()
        for (entity in world.players) {
            if (entity === player) {
                continue
            }

            if (player.squaredDistanceTo(entity) > rangeSquared) {
                notAlwaysInRadiusSet.add(entity.id)
            }

            if (Armor.enabled && !Armor.isValid(entity)) {
                armorSet.add(entity.id)
            }
        }

        armorSet.filterInPlace {
            val entity = world.getEntityById(it) as? PlayerEntity
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
            is EntityS2CPacket -> {
                if (!packet.isPositionChanged || !InvalidGround.enabled) {
                    return@handler
                }

                val entity = packet.getEntity(world) ?: return@handler
                val id = entity.id
                val currentValue = flyingSet.getOrDefault(id, 0)
                if (entity.isOnGround && entity.prevY != entity.y) {
                    flyingSet.put(id, currentValue + 1)
                } else if (!entity.isOnGround && currentValue > 0) {
                    val newVL = currentValue / 2

                    if (newVL <= 0) {
                        flyingSet.remove(id)
                    } else {
                        flyingSet.put(id, newVL)
                    }
                }
            }

            is EntityAttributesS2CPacket -> {
                attributesSet.add(packet.entityId)
            }

            is EntityAnimationS2CPacket -> {
                when (packet.animationId) {
                    EntityAnimationS2CPacket.SWING_MAIN_HAND, EntityAnimationS2CPacket.SWING_OFF_HAND -> {
                        swungSet.add(packet.entityId)
                    }
                    EntityAnimationS2CPacket.CRIT, EntityAnimationS2CPacket.ENCHANTED_HIT -> {
                        crittedSet.add(packet.entityId)
                    }
                }
            }

            is EntitiesDestroyS2CPacket -> {
                with(packet.entityIds.intIterator()) {
                    while (hasNext()) {
                        val entityId = nextInt()
                        attributesSet.remove(entityId)
                        flyingSet.remove(entityId)
                        hitSet.remove(entityId)
                        notAlwaysInRadiusSet.remove(entityId)
                        armorSet.remove(entityId)
                    }
                }
            }
        }
    }

    private fun hasInvalidGround(player: PlayerEntity): Boolean {
        return flyingSet.getOrDefault(player.id, 0) >= InvalidGround.vlToConsiderAsBot
    }

    override fun isBot(entity: PlayerEntity): Boolean {
        val entityId = entity.id
        return when {
            InvalidGround.enabled && hasInvalidGround(entity) -> true
            AlwaysInRadius.enabled && !notAlwaysInRadiusSet.contains(entityId) -> true
            Age.enabled && entity.age < Age.minimum -> true
            Armor.enabled && armorSet.contains(entityId) -> true
            else -> customConditions.any { it.isBot.test(entity) }
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

    private val VALID_CHARS_OF_NAME = BitSet(128).apply {
        set('0'.code, '9'.code + 1)
        set('a'.code, 'z'.code + 1)
        set('A'.code, 'Z'.code + 1)
        set('_'.code)
    }

    @Suppress("unused")
    private enum class CustomConditions(
        override val choiceName: String,
        val isBot: Predicate<PlayerEntity>
    ) : NamedChoice {
        DUPLICATE("Duplicate", { suspected ->
            isADuplicate(suspected.gameProfile)
        }),
        NO_GAME_MODE("NoGameMode", { suspected ->
            network.getPlayerListEntry(suspected.uuid)?.gameMode == null
        }),
        ILLEGAL_PITCH("IllegalPitch", { suspected ->
            abs(suspected.pitch) > 90
        }),
        FAKE_ENTITY_ID("FakeEntityID", { suspected ->
            suspected.id !in 0..1_000_000_000
        }),
        ILLEGAL_NAME("IllegalName", { suspected ->
            val name = suspected.nameForScoreboard
            name.length !in 3..16 || name.any { !VALID_CHARS_OF_NAME[it.code] }
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
        })
    }
}
