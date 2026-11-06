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
package net.ccbluex.liquidbounce.features.module.modules.render

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.kotlin.toUndashedString
import net.ccbluex.liquidbounce.utils.text.withFormat
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.projectile.Projectile
import java.util.UUID

/**
 * MobOwners module
 *
 * Shows you from which player a tamable entity or projectile belongs to.
 */

object ModuleMobOwners : ClientModule("MobOwners", ModuleCategories.RENDER) {

    private val projectiles by boolean("Projectiles", false)

    private val uuidNameCache = hashMapOf<UUID, FormattedCharSequence>()

    fun getOwnerInfoText(entity: Entity?): FormattedCharSequence? {
        if (entity == null || !this.running) {
            return null
        }

        val ownerId = when (entity) {
            is OwnableEntity -> entity.ownerReference?.uuid
            is Projectile if projectiles -> entity.owner?.uuid
            else -> null
        } ?: return null

        return world.getPlayerByUUID(ownerId)?.scoreboardName?.withFormat()
            ?: getFromMojangApi(ownerId)
    }

    private val LOADING_TEXT = "Loading..."
        .withFormat(Style.EMPTY.withItalic(true))

    private val FAILED_TEXT = "Failed to query Mojang API"
        .withFormat(Style.EMPTY.withItalic(true).withColor(ChatFormatting.RED))

    private val CANCELED_TEXT = "Query is canceled"
        .withFormat(Style.EMPTY.withItalic(true).withColor(ChatFormatting.YELLOW))

    @Suppress("SwallowedException")
    private fun getFromMojangApi(ownerId: UUID): FormattedCharSequence {
        return uuidNameCache.putIfAbsent(ownerId, LOADING_TEXT) ?: run {
            // The job will still run even if the module is disabled
            eventListenerScope.launch(Dispatchers.Main) {
                uuidNameCache[ownerId] = try {
                    val profile = HttpClient.mojangApiClient.mcServicesApi
                        .lookupNameByUuid(ownerId.toUndashedString())
                    profile.name.withFormat()
                } catch (_: CancellationException) {
                    CANCELED_TEXT
                } catch (_: Exception) {
                    FAILED_TEXT
                }
            }

            LOADING_TEXT
        }
    }

}
