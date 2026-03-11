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
package net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import java.util.function.Consumer

/**
 * This class represents a Fake Player implementing
 * attackability and assured totem pops instead of death
 * into [RemotePlayer].
 */
open class FakePlayer(
    level: ClientLevel,
    gameProfile: GameProfile,
    var onRemoval: Consumer<in FakePlayer>? = null,
) : RemotePlayer(level, gameProfile), MinecraftShortcuts {

    /**
     * Loads the attributes from the player into the fake player.
     */
    fun loadAttributes(snapshot: PosPoseSnapshot) {
        this.setPos(snapshot.x, snapshot.y, snapshot.z)
        this.xo = snapshot.lastX
        this.yo = snapshot.lastY
        this.zo = snapshot.lastZ
        this.xOld = snapshot.lastX
        this.yOld = snapshot.lastY
        this.zOld = snapshot.lastZ
        this.swinging = snapshot.handSwinging
        this.swingTime = snapshot.handSwingTicks
        this.attackAnim = snapshot.handSwingProgress
        this.yRot = snapshot.yaw
        this.yRotO = snapshot.lastYaw
        this.xRot = snapshot.pitch
        this.xRotO = snapshot.lastPitch
        this.yBodyRot = snapshot.bodyYaw
        this.yBodyRotO = snapshot.lastBodyYaw
        this.yHeadRot = snapshot.headYaw
        this.yHeadRotO = snapshot.lastHeadYaw
        this.pose = snapshot.pose
        this.swingingArm = snapshot.preferredHand
        this.inventory.replaceWith(snapshot.inventory)
        this.walkAnimation.position = snapshot.limbPos
    }

    override fun setHealth(health: Float) {
        super.setHealth(health)
        if (getHealth() <= 0f) {
            addEffect(MobEffectInstance(MobEffects.REGENERATION, 900, 1))
            addEffect(MobEffectInstance(MobEffects.ABSORPTION, 100, 1))
            addEffect(MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0))
            setHealth(1.0f)

            val packet = ClientboundEntityEventPacket(this, 35.toByte())
            val event = PacketEvent(TransferOrigin.INCOMING, packet, true)
            callEvent(event)
            if (!event.isCancelled) {
                mc.execute { packet.handle(mc.connection!!) }
            }
        }
    }

    /**
     * The fake player constantly checks for removal.
     */
    override fun tick() {
        if (removalReason != null) {
            onRemoval?.accept(this)
        }

        super.tick()

        if (tickCount % 10 == 0 && health < 20f) {
            health = (health + 0.5f).coerceAtMost(20f)
        }
    }

    /**
     * The fake player takes no knockback.
     */
    // this could perhaps be an option, but it could conflict with the recording
    override fun knockback(strength: Double, x: Double, z: Double) {
        /* nope */
    }

    override fun remove(reason: RemovalReason) {
        super.remove(reason)
    }

}
