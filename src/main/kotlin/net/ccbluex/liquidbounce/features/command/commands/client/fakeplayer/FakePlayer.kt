/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.client.fakeplayer

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.interfaces.OtherClientPlayerEntityAddition
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.damage.DamageSource

/**
 * This class represents a Fake Player implementing
 * attackability and assured totem pops instead of death
 * into [OtherClientPlayerEntity].
 */
open class FakePlayer(
    clientWorld: ClientWorld?,
    gameProfile: GameProfile?,
) : OtherClientPlayerEntity(
    clientWorld,
    gameProfile
) {

    lateinit var onRemoval: () -> Unit

    /**
     * Loads the attributes from the player into the fake player.
     */
    fun loadAttributes(snapshot: PosPoseSnapshot) {
        this.setPosition(snapshot.x, snapshot.y, snapshot.z)
        this.prevX = snapshot.prevX
        this.prevY = snapshot.prevY
        this.prevZ = snapshot.prevZ
        this.handSwinging = snapshot.handSwinging
        this.handSwingTicks = snapshot.handSwingTicks
        this.handSwingProgress = snapshot.handSwingProgress
        this.prevYaw = snapshot.yaw
        this.yaw = snapshot.prevYaw
        this.prevPitch = snapshot.pitch
        this.pitch = snapshot.prevPitch
        this.prevBodyYaw = snapshot.bodyYaw
        this.bodyYaw = snapshot.prevBodyYaw
        this.prevHeadYaw = snapshot.headYaw
        this.headYaw = snapshot.prevHeadYaw
        this.pose = snapshot.pose
        this.preferredHand = snapshot.preferredHand
        this.inventory.clone(snapshot.inventory)
        this.limbAnimator.pos = snapshot.limbPos
    }

    /**
     * Applies the actual damage.
     */
    override fun damage(source: DamageSource?, amount: Float): Boolean {
        @Suppress("CAST_NEVER_SUCCEEDS") // it does succeed with the mixin into OtherClientPlayerEntity
        return (this as OtherClientPlayerEntityAddition).`liquid_bounce$actuallyDamage`(source, amount)
    }

    /**
     * The fake player constantly checks for removal.
     */
    override fun tick() {
        if (removalReason != null) {
            onRemoval()
        }

        super.tick()
    }

    /**
     * The fake player takes no knockback.
     */
    // this could perhaps be an option, but it could conflict with the recording
    override fun takeKnockback(strength: Double, x: Double, z: Double) {
        /* nope */
    }

    override fun remove(reason: RemovalReason?) {
        super.remove(reason)
    }

}
