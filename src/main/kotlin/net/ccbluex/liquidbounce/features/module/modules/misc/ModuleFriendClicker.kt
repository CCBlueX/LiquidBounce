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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.NotificationEvent
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raytraceEntity
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text

/**
 * FriendClicker module
 *
 * Allows you to make friends by clicking on them.
 */

object ModuleFriendClicker : Module("FriendClicker", Category.MISC) {

    private val pickUpRange by float("PickUpRange", 3.0f, 1f..100f)

    private var clicked = false

    val repeatable = repeatable {
        val rotation = player.rotation

        val entity = (raytraceEntity(pickUpRange.toDouble(), rotation) { it is PlayerEntity }
            ?: return@repeatable) as PlayerEntity

        val facesEnemy = facingEnemy(entity, rotation, pickUpRange.toDouble(), wallsRange = 0.0)

        val pickup = mc.options.pickItemKey.isPressed

        if (facesEnemy && pickup && !clicked) {
            val name = entity.entityName

            if (FriendManager.isFriend(name)) {
                FriendManager.friends.remove(FriendManager.Friend(name, null))
                notification(
                    "Friend Clicker",
                    Text.translatable("$translationBaseKey.removedFriend", name),
                    NotificationEvent.Severity.INFO
                )
            } else {
                FriendManager.friends.add(FriendManager.Friend(name, null))

                notification(
                    "Friend Clicker",
                    Text.translatable("$translationBaseKey.addedFriend", name),
                    NotificationEvent.Severity.INFO
                )
            }
        }

        clicked = pickup
    }
}
