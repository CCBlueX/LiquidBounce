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
package net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket

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
), MinecraftShortcuts {

    var onRemoval: Runnable? = null

    /**
     * Loads the attributes from the player into the fake player.
     */
    fun loadAttributes(snapshot: PosPoseSnapshot) {
        this.setPosition(snapshot.x, snapshot.y, snapshot.z)
        this.lastX = snapshot.lastX
        this.lastY = snapshot.lastY
        this.lastZ = snapshot.lastZ
        this.handSwinging = snapshot.handSwinging
        this.handSwingTicks = snapshot.handSwingTicks
        this.handSwingProgress = snapshot.handSwingProgress
        this.lastYaw = snapshot.yaw
        this.yaw = snapshot.lastYaw
        this.lastPitch = snapshot.pitch
        this.pitch = snapshot.lastPitch
        this.lastBodyYaw = snapshot.bodyYaw
        this.bodyYaw = snapshot.lastBodyYaw
        this.lastHeadYaw = snapshot.headYaw
        this.headYaw = snapshot.lastHeadYaw
        this.pose = snapshot.pose
        this.preferredHand = snapshot.preferredHand
        this.inventory.clone(snapshot.inventory)
        this.limbAnimator.animationProgress = snapshot.limbPos
    }

    override fun setHealth(health: Float) {
        super.setHealth(health)
        if (getHealth() <= 0f) {
            addStatusEffect(StatusEffectInstance(StatusEffects.REGENERATION, 900, 1))
            addStatusEffect(StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1))
            addStatusEffect(StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0))
            setHealth(1.0f)

            val packet = EntityStatusS2CPacket(LivingEntity::class.java.cast(this), 35.toByte())
            val event = PacketEvent(TransferOrigin.INCOMING, packet, true)
            callEvent(event)
            if (!event.isCancelled) {
                mc.execute { packet.apply(mc.networkHandler) }
            }
        }
    }

    /**
     * The fake player constantly checks for removal.
     */
    override fun tick() {
        if (removalReason != null) {
            onRemoval?.run()
        }

        super.tick()

        if (age % 10 == 0 && health < 20f) {
            health = (health + 0.5f).coerceAtMost(20f)
        }
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
