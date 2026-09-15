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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.minecraft.world.entity.Entity

/**
 * Friends are shared with every client module. A change fires
 * [net.ccbluex.liquidbounce.event.events.FriendChangeEvent].
 */
object Friends {

    @JvmStatic
    fun isFriend(name: String): Boolean = FriendManager.isFriend(name)

    @JvmStatic
    fun isFriend(entity: Entity): Boolean = FriendManager.isFriend(entity)

    @JvmStatic
    fun names(): List<String> = FriendManager.friends.map(FriendManager.Friend::name)

    @JvmStatic
    @JvmOverloads
    fun add(name: String, alias: String? = null): Boolean = FriendManager.add(FriendManager.Friend(name, alias))

    @JvmStatic
    fun remove(name: String): Boolean = FriendManager.remove(name)


}
