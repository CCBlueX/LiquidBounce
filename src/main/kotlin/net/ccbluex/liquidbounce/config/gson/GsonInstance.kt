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
 *
 *
 */

package net.ccbluex.liquidbounce.config.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.config.gson.adapter.*
import net.ccbluex.liquidbounce.config.gson.serializer.*
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.*
import net.ccbluex.liquidbounce.config.gson.stategies.ExcludeStrategy
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExcludeStrategy
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.block.Block
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.session.Session
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundEvent
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.GameMode
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
    .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ConfigurableSerializer.FILE_SERIALIZER)
    .create()

/**
 * A GSON instance which is used for JSON that is distributed to other players.
 */
val publicGson: Gson = GsonBuilder()
    .setPrettyPrinting()
    .addSerializationExclusionStrategy(ExcludeStrategy)
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ConfigurableSerializer.PUBLIC_SERIALIZER)
    .create()

/**
 * This GSON instance is used for interop communication.
 */
internal val interopGson: Gson = GsonBuilder()
    .addSerializationExclusionStrategy(ProtocolExcludeStrategy)
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ConfigurableSerializer.INTEROP_SERIALIZER)
    .create()

/**
 * This GSON instance is used for serializing objects as accessible JSON which means it is READ-ONLY (!)
 * and often comes with an easier syntax to use in other programming languages like JavaScript.
 */
internal val accessibleInteropGson: Gson = GsonBuilder()
    .addSerializationExclusionStrategy(ProtocolExcludeStrategy)
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ConfigurableSerializer.INTEROP_SERIALIZER)
    .registerTypeHierarchyAdapter(Component::class.javaObjectType, ReadOnlyComponentSerializer)
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
        .registerTypeHierarchyAdapter(ClosedRange::class.javaObjectType, RangeAdapter)
        .registerTypeHierarchyAdapter(IntRange::class.javaObjectType, IntRangeAdapter)
        .registerTypeHierarchyAdapter(Item::class.javaObjectType, ItemAdapter)
        .registerTypeHierarchyAdapter(SoundEvent::class.javaObjectType, SoundEventAdapter)
        .registerTypeHierarchyAdapter(StatusEffect::class.javaObjectType, StatusEffectAdapter)
        .registerTypeHierarchyAdapter(Color4b::class.javaObjectType, ColorAdapter)
        .registerTypeHierarchyAdapter(Vec3d::class.javaObjectType, Vec3dAdapter)
        .registerTypeHierarchyAdapter(Vec3i::class.javaObjectType, Vec3iAdapter)
        .registerTypeHierarchyAdapter(Vec2f::class.javaObjectType, Vec2fAdapter)
        .registerTypeHierarchyAdapter(Block::class.javaObjectType, BlockAdapter)
        .registerTypeHierarchyAdapter(InputUtil.Key::class.javaObjectType, InputUtilAdapter)
        .registerTypeHierarchyAdapter(InputBind::class.javaObjectType, InputBindAdapter)
        .registerTypeAdapter(ChoiceConfigurable::class.javaObjectType, ChoiceConfigurableSerializer)
        .registerTypeHierarchyAdapter(NamedChoice::class.javaObjectType, EnumChoiceSerializer)
        .registerTypeHierarchyAdapter(MinecraftAccount::class.javaObjectType, MinecraftAccountAdapter)
        .registerTypeHierarchyAdapter(Text::class.javaObjectType, TextSerializer)
        .registerTypeHierarchyAdapter(Screen::class.javaObjectType, ScreenSerializer)
        .registerTypeAdapter(Session::class.javaObjectType, SessionSerializer)
        .registerTypeAdapter(ServerInfo::class.javaObjectType, ServerInfoSerializer)
        .registerTypeAdapter(GameMode::class.javaObjectType, GameModeSerializer)
        .registerTypeAdapter(ItemStack::class.javaObjectType, ItemStackSerializer)
        .registerTypeAdapter(Identifier::class.javaObjectType, IdentifierAdapter)
        .registerTypeAdapter(StatusEffectInstance::class.javaObjectType, StatusEffectInstanceSerializer)
        .registerTypeHierarchyAdapter(Supplier::class.javaObjectType, SupplierSerializer)
