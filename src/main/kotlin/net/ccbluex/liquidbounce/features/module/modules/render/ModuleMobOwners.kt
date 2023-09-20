/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.HorseEntity
import net.minecraft.entity.passive.TameableEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.util.Formatting
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * MobOwners module
 *
 * Shows you from which player a tamable entity or projectile belongs to.
 */

object ModuleMobOwners : Module("MobOwners", Category.RENDER) {

    val projectiles by boolean("Projectiles", false)

    val uuidNameCache = ConcurrentHashMap<UUID, OrderedText>()

    var asyncRequestExecutor = Executors.newSingleThreadExecutor()

    fun getOwnerInfoText(entity: Entity): OrderedText? {
        if (!this.enabled) {
            return null
        }

        val ownerId = when {
            entity is TameableEntity -> entity.ownerUuid
            entity is HorseEntity -> entity.ownerUuid
            entity is ProjectileEntity && projectiles -> entity.ownerUuid
            else -> null
        } ?: return null

        return world.getPlayerByUuid(ownerId)
            ?.let { OrderedText.styledForwardsVisitedString(it.entityName, Style.EMPTY) }
            ?: getFromMojangApi(ownerId)
    }

    private fun getFromMojangApi(ownerId: UUID): OrderedText {
        return uuidNameCache.computeIfAbsent(ownerId) {
            this.asyncRequestExecutor.submit {
                try {
                    class UsernameRecord(var name: String, var changedToAt: Int?)

                    val response = decode<Array<UsernameRecord>>(HttpClient.get("https://api.mojang.com/user/profiles/${it.toString().replace("-", "")}/names"))

                    val entityName = response.first { it.changedToAt == null }.name

                    uuidNameCache[it] = OrderedText.styledForwardsVisitedString(entityName, Style.EMPTY)
                } catch (e: InterruptedException) {
                } catch (e: Exception) {
                    uuidNameCache[it] = OrderedText.styledForwardsVisitedString("Failed to query Mojang API", Style.EMPTY.withItalic(true).withColor(Formatting.RED))
                }
            }

            OrderedText.styledForwardsVisitedString("Loading", Style.EMPTY.withItalic(true))
        }
    }

    override fun disable() {
        this.asyncRequestExecutor.shutdownNow()

        this.asyncRequestExecutor = Executors.newSingleThreadExecutor()
    }

}
