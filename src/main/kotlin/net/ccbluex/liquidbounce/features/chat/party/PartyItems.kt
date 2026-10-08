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
import com.google.gson.JsonPrimitive
import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.features.chat.packet.PartyMember
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.inventory.ViewedInventoryScreen
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack

private const val MAX_NAME = 64
private const val MAX_COUNT = 99
private const val MAX_ENCHANTMENT_LEVEL = 255

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
                addProperty("displayName", customName.string.take(MAX_NAME))
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

    /**
     * Members can send anything: unknown parts are dropped, numbers clamped, and names stay plain text,
     * never components with events or deep nesting.
     */
    fun decode(json: JsonElement?, level: ClientLevel): ItemStack {
        val item = json?.takeIf { it.isJsonObject }?.asJsonObject ?: return ItemStack.EMPTY
        val identifier = item.text("identifier")?.let(Identifier::tryParse) ?: return ItemStack.EMPTY
        val count = item.number("count")?.toInt()?.coerceIn(1, MAX_COUNT) ?: 1
        val stack = ItemStack(BuiltInRegistries.ITEM.getValue(identifier), count)

        item.number("damage")?.toInt()?.let(stack::setDamageValue)
        val name = item["displayName"]?.let { name -> if (name.isJsonObject) name.asJsonObject.text("text") else null }
            ?: item.text("displayName")
        name?.let { stack.set(DataComponents.CUSTOM_NAME, Component.literal(it.take(MAX_NAME))) }

        val enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        item["enchantments"]?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (id, enchantmentLevel) ->
            val key = Identifier.tryParse(id) ?: return@forEach
            val value = (enchantmentLevel as? JsonPrimitive)?.takeIf { it.isNumber }?.asInt ?: return@forEach
            enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, key))
                .ifPresent { stack.enchant(it, value.coerceIn(1, MAX_ENCHANTMENT_LEVEL)) }
        }

        return stack
    }

    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.asString

    private fun JsonObject.number(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isNumber }?.asDouble
        ?.takeIf { it.isFinite() }

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
