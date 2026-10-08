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


package net.ccbluex.liquidbounce.features.chat.party

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import com.mojang.serialization.JsonOps
import net.ccbluex.liquidbounce.features.chat.packet.PartyMember
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.inventory.ViewedInventoryScreen
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack

/**
 * The compact item shape of party states: LiquidBounce's item JSON without default fields,
 * and `null` for empty slots.
 */
object PartyItems {

    fun encode(stack: ItemStack, withName: Boolean = true): JsonElement {
        if (stack.isEmpty) {
            return JsonNull.INSTANCE
        }

        return JsonObject().apply {
            addProperty("identifier", BuiltInRegistries.ITEM.getKey(stack.item).toString())
            addProperty("count", stack.count)
            if (stack.isDamageableItem) {
                addProperty("damage", stack.damageValue)
                addProperty("maxDamage", stack.maxDamage)
            }

            val customName = stack.customName
            if (withName && customName != null) {
                ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, customName).result()
                    .ifPresent { add("displayName", it) }
            }

            val enchantments = stack.enchantments.entrySet()
            if (enchantments.isNotEmpty()) {
                add("enchantments", JsonObject().apply {
                    for (entry in enchantments) {
                        addProperty(entry.key.registeredName, entry.intValue)
                    }
                })
            }
        }
    }

    fun encode(stacks: Iterable<ItemStack>, withName: Boolean = true) = JsonArray().apply {
        stacks.forEach { add(encode(it, withName)) }
    }

    fun decode(json: JsonElement?, level: ClientLevel): ItemStack {
        val item = json?.takeIf { it.isJsonObject }?.asJsonObject ?: return ItemStack.EMPTY
        val identifier = item["identifier"]?.asString?.let(Identifier::tryParse) ?: return ItemStack.EMPTY
        val stack = ItemStack(BuiltInRegistries.ITEM.getValue(identifier), item["count"]?.asInt ?: 1)

        item["damage"]?.asInt?.let(stack::setDamageValue)
        item["displayName"]?.let { name ->
            ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, name).result()
                .ifPresent { stack.set(DataComponents.CUSTOM_NAME, it) }
        }

        val enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        item["enchantments"]?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (id, enchantmentLevel) ->
            val key = Identifier.tryParse(id) ?: return@forEach
            enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, key))
                .ifPresent { stack.enchant(it, enchantmentLevel.asInt) }
        }

        return stack
    }

    fun show(member: PartyMember, inventory: JsonObject) {
        val level = mc.level ?: return
        val viewed = viewedPlayer(member, inventory, level)
        mc.schedule {
            mc.gui.setScreen(ViewedInventoryScreen { viewed })
        }
    }

    private fun viewedPlayer(member: PartyMember, inventory: JsonObject, level: ClientLevel): RemotePlayer {
        val profile = member.player?.let { GameProfile(it.uuid, it.name) }
            ?: GameProfile(member.user.uuid, member.user.name)
        val player = RemotePlayer(level, profile).apply { id = level.nextLocalEntityId() }

        inventory["main"]?.takeIf { it.isJsonArray }?.asJsonArray?.forEachIndexed { slot, item ->
            player.inventory.setItem(slot, decode(item, level))
        }
        val armor = inventory["armor"]?.takeIf { it.isJsonArray }?.asJsonArray
        val armorSlots = listOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD)
        armor?.forEachIndexed { index, item ->
            armorSlots.getOrNull(index)?.let { player.setItemSlot(it, decode(item, level)) }
        }
        player.setItemSlot(EquipmentSlot.OFFHAND, decode(inventory["offhand"], level))

        return player
    }

}
