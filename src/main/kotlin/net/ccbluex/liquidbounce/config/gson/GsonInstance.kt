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

package net.ccbluex.liquidbounce.config.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.config.gson.adapter.AlignmentAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.CodecBasedAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.ColorAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.IdentifierWithRegistryAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.InputBindAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.IntRangeAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.LocalDateAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.LocalDateTimeAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.MinecraftAccountAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.OffsetDateTimeAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.OptionalAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.RangeAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.SimpleStringTypeAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.Vec2fAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.Vec3dAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.Vec3iAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.Vector2fcAdapter
import net.ccbluex.liquidbounce.config.gson.serializer.ModeValueGroupSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.ReadOnlyComponentSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.ReadOnlyThemeSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.SupplierSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.TaggedSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.ValueGroupSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.ItemStackSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.ScreenSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.ServerInfoSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.SessionSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.StatusEffectInstanceSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.StringRepresentableSerializer
import net.ccbluex.liquidbounce.config.gson.stategies.ExcludeStrategy
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExcludeStrategy
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.integration.theme.Theme
import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.User
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.core.Vec3i
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.StringRepresentable
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EntityType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Vector2fc
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.function.Supplier

/**
 * A GSON instance which is used for local files.
 */
val fileGson: Gson = GsonBuilder()
    .addSerializationExclusionStrategy(ExcludeStrategy)
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(ValueGroup::class.javaObjectType, ValueGroupSerializer.FILE_SERIALIZER)
    .create()

/**
 * A GSON instance which is used for JSON that is distributed to other players.
 */
val publicGson: Gson = GsonBuilder()
    .setPrettyPrinting()
    .addSerializationExclusionStrategy(ExcludeStrategy)
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(ValueGroup::class.javaObjectType, ValueGroupSerializer.PUBLIC_SERIALIZER)
    .create()

/**
 * This GSON instance is used for interop communication.
 */
internal val interopGson: Gson = GsonBuilder()
    .addSerializationExclusionStrategy(ProtocolExcludeStrategy)
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(ValueGroup::class.javaObjectType, ValueGroupSerializer.INTEROP_SERIALIZER)
    .create()

/**
 * This GSON instance is used for serializing objects as accessible JSON which means it is READ-ONLY (!)
 * and often comes with an easier syntax to use in other programming languages like JavaScript.
 */
internal val accessibleInteropGson: Gson = GsonBuilder()
    .addSerializationExclusionStrategy(ProtocolExcludeStrategy)
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(ValueGroup::class.javaObjectType, ValueGroupSerializer.INTEROP_SERIALIZER)
    .registerTypeHierarchyAdapter(Theme::class.javaObjectType, ReadOnlyThemeSerializer)
    .registerTypeHierarchyAdapter(HudComponent::class.javaObjectType, ReadOnlyComponentSerializer)
    .registerTypeHierarchyAdapter(Alignment::class.javaObjectType, AlignmentAdapter)
    .create()

/**
 * Register common type adapters
 * These adapters include anything from Kotlin classes to Minecraft and LiquidBounce types
 * They are safe to use on any GSON instance. (clientGson, autoConfigGson, ...)
 * It does not include any configurable serializers, which means you need to add them yourself if needed!
 *
 * @see GsonBuilder.registerTypeHierarchyAdapter
 * @see GsonBuilder.registerTypeAdapter
 */
internal fun GsonBuilder.registerCommonTypeAdapters() =
    registerTypeAdapter(LocalDate::class.java, LocalDateAdapter)
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter)
        .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeAdapter)
        .registerTypeAdapter(Regex::class.java, SimpleStringTypeAdapter.KT_REGEX)
        .registerTypeHierarchyAdapter(ClosedRange::class.javaObjectType, RangeAdapter)
        .registerTypeHierarchyAdapter(IntRange::class.javaObjectType, IntRangeAdapter)
        .registerTypeHierarchyAdapter(File::class.java, SimpleStringTypeAdapter.FILE)
        .registerTypeHierarchyAdapter(EntityType::class.java, IdentifierWithRegistryAdapter.ENTITY_TYPE)
        .registerTypeHierarchyAdapter(Item::class.javaObjectType, IdentifierWithRegistryAdapter.ITEM)
        .registerTypeAdapter(DataComponentPatch::class.java, CodecBasedAdapter.DATA_COMPONENT_PATCH)
        .registerTypeHierarchyAdapter(SoundEvent::class.javaObjectType, IdentifierWithRegistryAdapter.SOUND_EVENT)
        .registerTypeHierarchyAdapter(MobEffect::class.javaObjectType, IdentifierWithRegistryAdapter.STATUS_EFFECT)
        .registerTypeHierarchyAdapter(MenuType::class.java, IdentifierWithRegistryAdapter.SCREEN_HANDLER)
        .registerTypeHierarchyAdapter(Color4b::class.javaObjectType, ColorAdapter)
        .registerTypeHierarchyAdapter(Vec3::class.javaObjectType, Vec3dAdapter)
        .registerTypeHierarchyAdapter(Vec3i::class.javaObjectType, Vec3iAdapter)
        .registerTypeHierarchyAdapter(Vec2::class.javaObjectType, Vec2fAdapter)
        .registerTypeHierarchyAdapter(Vector2fc::class.java, Vector2fcAdapter)
        .registerTypeHierarchyAdapter(Block::class.javaObjectType, IdentifierWithRegistryAdapter.BLOCK)
        .registerTypeHierarchyAdapter(InputConstants.Key::class.javaObjectType, SimpleStringTypeAdapter.INPUT_KEY)
        .registerTypeHierarchyAdapter(InputBind::class.javaObjectType, InputBindAdapter)
        .registerTypeAdapter(ModeValueGroup::class.javaObjectType, ModeValueGroupSerializer)
        .registerTypeHierarchyAdapter(Tagged::class.javaObjectType, TaggedSerializer)
        .registerTypeHierarchyAdapter(MinecraftAccount::class.javaObjectType, MinecraftAccountAdapter)
        .registerTypeHierarchyAdapter(Component::class.javaObjectType, CodecBasedAdapter.TRANSLATED_COMPONENT)
        .registerTypeHierarchyAdapter(Screen::class.javaObjectType, ScreenSerializer)
        .registerTypeHierarchyAdapter(User::class.javaObjectType, SessionSerializer)
        .registerTypeAdapter(ServerData::class.javaObjectType, ServerInfoSerializer)
        .registerTypeHierarchyAdapter(StringRepresentable::class.java, StringRepresentableSerializer)
        .registerTypeAdapter(ItemStack::class.javaObjectType, ItemStackSerializer)
        .registerTypeAdapter(Identifier::class.javaObjectType, SimpleStringTypeAdapter.IDENTIFIER)
        .registerTypeAdapter(MobEffectInstance::class.javaObjectType, StatusEffectInstanceSerializer)
        .registerTypeHierarchyAdapter(Supplier::class.javaObjectType, SupplierSerializer)
        .registerTypeAdapterFactory(OptionalAdapter)
