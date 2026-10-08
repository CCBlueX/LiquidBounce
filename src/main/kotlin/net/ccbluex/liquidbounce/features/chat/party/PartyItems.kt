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

import com.mojang.authlib.GameProfile
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.ccbluex.axochat.party.PartyMember
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
            return JsonNull
        }

        return buildJsonObject {
            put("identifier", BuiltInRegistries.ITEM.getKey(stack.item).toString())
            put("count", stack.count)
            if (stack.isDamageableItem) {
                put("damage", stack.damageValue)
                put("maxDamage", stack.maxDamage)
            }

            val customName = stack.customName
            if (withName && customName != null) {
                put("displayName", customName.string.take(MAX_NAME))
            }

            val enchantments = stack.enchantments.entrySet()
            if (enchantments.isNotEmpty()) {
                putJsonObject("enchantments") {
                    for (entry in enchantments) {
                        put(entry.key.registeredName, entry.intValue)
                    }
                }
            }
        }
    }

    fun encode(stacks: Iterable<ItemStack>, withName: Boolean = true) = buildJsonArray {
        stacks.forEach { add(encode(it, withName)) }
    }

    /**
     * Members can send anything: unknown parts are dropped, numbers clamped, and names stay plain text,
     * never components with events or deep nesting.
     */
    fun decode(json: JsonElement?, level: ClientLevel): ItemStack {
        val item = json as? JsonObject ?: return ItemStack.EMPTY
        val identifier = item.text("identifier")?.let(Identifier::tryParse) ?: return ItemStack.EMPTY
        val count = item.number("count")?.toInt()?.coerceIn(1, MAX_COUNT) ?: 1
        val stack = ItemStack(BuiltInRegistries.ITEM.getValue(identifier), count)

        item.number("damage")?.toInt()?.let(stack::setDamageValue)
        val name = (item["displayName"] as? JsonObject)?.text("text") ?: item.text("displayName")
        name?.let { stack.set(DataComponents.CUSTOM_NAME, Component.literal(it.take(MAX_NAME))) }

        val enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        (item["enchantments"] as? JsonObject)?.forEach { (id, enchantmentLevel) ->
            val key = Identifier.tryParse(id) ?: return@forEach
            val value = (enchantmentLevel as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull ?: return@forEach
            enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, key))
                .ifPresent { stack.enchant(it, value.coerceIn(1, MAX_ENCHANTMENT_LEVEL)) }
        }

        return stack
    }

    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String) = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull
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

        (inventory["main"] as? JsonArray)?.forEachIndexed { slot, item ->
            player.inventory.setItem(slot, decode(item, level))
        }
        val armor = inventory["armor"] as? JsonArray
        val armorSlots = listOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD)
        armor?.forEachIndexed { index, item ->
            armorSlots.getOrNull(index)?.let { player.setItemSlot(it, decode(item, level)) }
        }
        player.setItemSlot(EquipmentSlot.OFFHAND, decode(inventory["offhand"], level))

        return player
    }

}
