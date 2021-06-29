/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.EntityHitResult

object ModuleFriendClicker : Module("FriendClicker", Category.MISC) {

    var clicked = false

    val repeatable = repeatable {
        val crosshair = mc.crosshairTarget

        if (crosshair is EntityHitResult && crosshair.entity is PlayerEntity && mc.options.keyPickItem.isPressed && !clicked) {
            val name = (crosshair.entity as PlayerEntity).gameProfile.name

            if (FriendManager.isFriend(name)) {
                FriendManager.friends.remove(FriendManager.Friend(name, null))
                chat("§8$name §7was successfully removed from the friend list.")
            } else {
                FriendManager.friends.add(FriendManager.Friend(name, null))
                chat("§8$name §7was successfully added to the friend list.")
            }
        }
        clicked = mc.options.keyPickItem.isPressed
    }
}
