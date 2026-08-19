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

package net.ccbluex.liquidbounce.features.module.modules.render.wings

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations
import net.ccbluex.liquidbounce.features.module.modules.render.wings.ModuleWings.WingsPosition
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.minecraft.util.Mth.rotLerp
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.phys.Vec3

abstract class WingsMode(name: String) : Mode(name) {
    final override val parent: ModeValueGroup<*>
        get() = ModuleWings.modes

    val showDamage by boolean("ShowDamage", true)
    val showInFirstPerson by boolean("ShowInFirstPerson", false)

    private class FriendsOptions : ValueGroup("FriendsOptions") {
        val friendView by boolean("ViewOnFriend", true)
        val distance by int("Distance", 64, 8..512, "blocks")
    }

    protected abstract fun WorldRenderEnvironment.drawWings(isHurt: Boolean, bodyRot: Float, shifting: Float)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val player = mc.player ?: return@handler

        event.renderEnvironment {
            for (entity in world.players()) {

                val isMe = entity == player
                val isFriend =
                    FriendsOptions().friendView
                        && FriendManager.isFriend(entity)
                        && player.distanceTo(entity) <= FriendsOptions().distance

                val shouldRender = when {
                    isMe -> !mc.options.cameraType.isFirstPerson || showInFirstPerson || ModuleFreeLook.enabled
                    isFriend -> true
                    else -> false
                }

                if (!shouldRender) continue

                val partAllowed = ModuleRotations.isPartAllowed(ModuleRotations.BodyPart.BODY)

                val bodyRot = if (isMe && ModuleRotations.running && partAllowed) {
                    val modelRot = ModuleRotations.modelRotation?.yaw
                    val prevModelRot = ModuleRotations.prevModelRotation?.yaw
                    rotLerp(event.partialTicks, prevModelRot ?: entity.yBodyRotO, modelRot ?: entity.yBodyRot)
                } else { rotLerp(event.partialTicks, entity.yBodyRotO, entity.yBodyRot) }

                val look = Vec3.directionFromRotation(0f, bodyRot)
                val equipmentOffset = when (!entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty) {
                    true -> WingsPosition.equipmentOffset.toDouble()
                    else -> 0.0
                }

                val hurtMarked = entity.hurtTime != 0 && showDamage
                val shifting = when (player.isShiftKeyDown) {
                    true -> 0.1f
                    false -> 0f
                }

                val behind = WingsPosition.behindScale.toDouble() + equipmentOffset
                val pos = entity.getPosition(event.partialTicks).subtract(look.scale(behind))

                withPositionRelativeToCamera(pos.add(
                    0.0,
                    (entity.bbHeight / 2.0) + WingsPosition.wingsHeight - shifting,
                    0.0
                )) {
                    poseStack.withPush {
                        drawWings(hurtMarked, bodyRot, shifting)
                    }
                }
            }
        }
    }
}
