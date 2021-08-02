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

import net.ccbluex.liquidbounce.event.NotificationEvent
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.TranslatableText
import net.minecraft.util.hit.EntityHitResult

/**
 * FriendClicker module
 *
 * Allows you to make friends by clicking on them.
 */

object ModuleFriendClicker : Module("FriendClicker", Category.MISC) {

    private var clicked = false

    val repeatable = repeatable {
        val crosshair = mc.crosshairTarget
        val pickup = mc.options.keyPickItem.isPressed

        if (crosshair is EntityHitResult && crosshair.entity is PlayerEntity && pickup && !clicked) {
            val name = (crosshair.entity as PlayerEntity).gameProfile.name

            if (FriendManager.isFriend(name)) {
                FriendManager.friends.remove(FriendManager.Friend(name, null))
                notification(
                    "Friend Clicker",
                    TranslatableText("$translationBaseKey.removedFriend", name),
                    NotificationEvent.Severity.INFO
                )
            } else {
                FriendManager.friends.add(FriendManager.Friend(name, null))

                notification(
                    "Friend Clicker",
                    TranslatableText("$translationBaseKey.addedFriend", name),
                    NotificationEvent.Severity.INFO
                )
            }
        }

        clicked = pickup
    }
}
