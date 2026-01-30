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
package net.ccbluex.liquidbounce.features.creativetab.tabs

import com.google.common.collect.ImmutableMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.api.core.retrying
import net.ccbluex.liquidbounce.features.creativetab.CustomCreativeModeTab
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.ResolvableProfile
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

data class Head(val name: String, val uuid: UUID, val value: String) {

    fun asItemStack(): ItemStack {
        val builder = DataComponentPatch.builder()

        builder.set(DataComponents.CUSTOM_NAME, name.asPlainText(CUSTOM_NAME_STYLE))
        builder.set(
            DataComponents.LORE,
            ItemLore(
                listOf(
                    "UUID: $uuid".asPlainText(UUID_STYLE),
                    "liquidbounce.net".asPlainText(CLIENT_LINK_STYLE),
                )
            )
        )

        val profile = GameProfile(
            uuid,
            name,
            PropertyMap(
                ImmutableMultimap.of("textures", Property("textures", value))
            ),
        )

        builder.set(
            DataComponents.PROFILE,
            ResolvableProfile.createResolved(profile),
        )

        return ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(Items.PLAYER_HEAD), 1, builder.build())
    }


    companion object {
        @JvmStatic
        private val CUSTOM_NAME_STYLE = Style.EMPTY.applyFormats(ChatFormatting.GOLD, ChatFormatting.BOLD)

        @JvmStatic
        private val UUID_STYLE = Style.EMPTY.applyFormat(ChatFormatting.GRAY)

        @JvmStatic
        private val CLIENT_LINK_STYLE = Style.EMPTY.withColor(Color4b.LIQUID_BOUNCE.argb)
    }
}

class HeadsCreativeModeTab : CustomCreativeModeTab(
    "Heads",
    icon = { Items.SKELETON_SKULL.defaultInstance },
    items = { items ->
        heads.getNow()?.let { heads ->
            items.acceptAll(heads.distinctBy { it.name }.map(Head::asItemStack))
        }
    }
) {
    companion object {
        /**
         * The API endpoint to fetch heads from which is owned by CCBlueX
         * and therefore can reliably depend on.
         */
        const val HEAD_DB_API = "https://headdb.org/api/category/all"

        val heads = ioScope.retrying(
            interval = 1.seconds,
            name = "player-heads",
            maxRetries = 3,
        ) {
            val heads: HashMap<String, Head> = HttpClient.request(HEAD_DB_API, HttpMethod.GET).parse()

            heads.values.also {
                logger.info("Successfully loaded ${it.size} heads from HeadDB")
            } as Collection<Head>
        }
    }
}
