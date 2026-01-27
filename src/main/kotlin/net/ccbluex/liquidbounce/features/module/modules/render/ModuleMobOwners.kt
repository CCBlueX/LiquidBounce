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
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.thirdparty.MojangApi
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.animal.equine.AbstractHorse
import net.minecraft.world.entity.projectile.Projectile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * MobOwners module
 *
 * Shows you from which player a tamable entity or projectile belongs to.
 */

object ModuleMobOwners : ClientModule("MobOwners", ModuleCategories.RENDER) {

    private val projectiles by boolean("Projectiles", false)

    private val uuidNameCache = ConcurrentHashMap<UUID, FormattedCharSequence>()

    fun getOwnerInfoText(entity: Entity?): FormattedCharSequence? {
        if (entity == null || !this.running) {
            return null
        }

        val ownerId = when (entity) {
            is TamableAnimal -> entity.ownerReference?.uuid
            is AbstractHorse -> entity.ownerReference?.uuid
            is Projectile if projectiles -> entity.owner?.uuid
            else -> null
        } ?: return null

        return world.getPlayerByUUID(ownerId)
            ?.let { FormattedCharSequence.forward(it.scoreboardName, Style.EMPTY) }
            ?: getFromMojangApi(ownerId)
    }

    private val LOADING_TEXT = FormattedCharSequence.forward(
        "Loading...",
        Style.EMPTY.withItalic(true)
    )

    private val FAILED_TEXT = FormattedCharSequence.forward(
        "Failed to query Mojang API",
        Style.EMPTY.withItalic(true).withColor(ChatFormatting.RED)
    )

    private val CANCELED_TEXT = FormattedCharSequence.forward(
        "Query is canceled",
        Style.EMPTY.withItalic(true).withColor(ChatFormatting.YELLOW)
    )

    @Suppress("SwallowedException")
    private fun getFromMojangApi(ownerId: UUID): FormattedCharSequence {
        return uuidNameCache.putIfAbsent(ownerId, LOADING_TEXT) ?: run {
            // The job will still run even if the module is disabled
            withScope {
                uuidNameCache[ownerId] = try {
                    val uuidAsString = ownerId.toString().replace("-", "")
                    val response = MojangApi.getNames(uuidAsString)

                    val entityName = response.first { it.changedToAt == null }.name

                    FormattedCharSequence.forward(entityName, Style.EMPTY)
                } catch (e: CancellationException) {
                    CANCELED_TEXT
                } catch (e: Exception) {
                    FAILED_TEXT
                }
            }

            LOADING_TEXT
        }
    }

}
