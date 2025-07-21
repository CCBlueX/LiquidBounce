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
package net.ccbluex.liquidbounce.features.misc

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import java.util.*

object FriendManager : Configurable("Friends"), EventListener {

    val friends by list(name, TreeSet<Friend>(), valueType = ValueType.FRIEND)

    @Suppress("unused")
    private val tagEntityEvent = handler<TagEntityEvent> {
        if (isFriend(it.entity)) {
            it.assumeFriend()
        }
    }

    init {
        ConfigSystem.root(this)
    }

    class Friend(val name: String, var alias: String?) : Comparable<Friend> {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Friend

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun compareTo(other: Friend): Int = this.name.compareTo(other.name)

        fun getDefaultName(id: Int): String = "Friend $id"

    }

    fun isFriend(name: String): Boolean = friends.contains(Friend(name, null))
    fun isFriend(entity: Entity): Boolean = entity is PlayerEntity && isFriend(entity.gameProfile.name)

}
