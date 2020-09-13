/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.entity.player.IInventoryPlayer
import net.ccbluex.liquidbounce.api.minecraft.entity.player.IPlayerCapabilities
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.stats.IStatBase
import net.ccbluex.liquidbounce.api.minecraft.util.IFoodStats
import net.minecraft.entity.player.EntityPlayer

open class EntityPlayerImpl<T : EntityPlayer>(wrapped: T) : EntityLivingBaseImpl<T>(wrapped), IEntityPlayer {
    override val gameProfile: GameProfile
        get() = wrapped.gameProfile
    override val fishEntity: IEntity?
        get() = wrapped.fishEntity?.wrap()
    override val foodStats: IFoodStats
        get() = wrapped.foodStats.wrap()
    override val prevChasingPosY: Double
        get() = wrapped.prevChasingPosY
    override var sleepTimer: Int
        get() = wrapped.sleepTimer
        set(value) {
            wrapped.sleepTimer = value
        }
    override var sleeping: Boolean
        get() = wrapped.sleeping
        set(value) {
            wrapped.sleeping = value
        }
    override val isPlayerSleeping: Boolean
        get() = wrapped.isPlayerSleeping
    override var speedInAir: Float
        get() = wrapped.speedInAir
        set(value) {
            wrapped.speedInAir = value
        }
    override var cameraYaw: Float
        get() = wrapped.cameraYaw
        set(value) {
            wrapped.cameraYaw = value
        }
    override val isBlocking: Boolean
        get() = wrapped.isBlocking
    override var itemInUseCount: Int
        get() = wrapped.itemInUseCount
        set(value) {
            wrapped.itemInUseCount = value
        }
    override val itemInUse: IItemStack?
        get() = wrapped.itemInUse?.wrap()
    override val capabilities: IPlayerCapabilities
        get() = wrapped.capabilities.wrap()
    override val heldItem: IItemStack?
        get() = wrapped.heldItem?.wrap()
    override val isUsingItem: Boolean
        get() = wrapped.isUsingItem
    override val inventoryContainer: IContainer
        get() = wrapped.inventoryContainer.wrap()
    override val inventory: IInventoryPlayer
        get() = wrapped.inventory.wrap()
    override val openContainer: IContainer?
        get() = wrapped.openContainer?.wrap()
    override val itemInUseDuration: Int
        get() = wrapped.itemInUseDuration
    override val displayNameString: String
        get() = wrapped.displayNameString
    override val spectator: Boolean
        get() = wrapped.isSpectator

    override fun stopUsingItem() = wrapped.stopUsingItem()

    override fun onCriticalHit(entity: IEntity) = wrapped.onCriticalHit(entity.unwrap())

    override fun onEnchantmentCritical(entity: IEntityLivingBase) = wrapped.onEnchantmentCritical(entity.unwrap())

    override fun attackTargetEntityWithCurrentItem(entity: IEntity) = wrapped.attackTargetEntityWithCurrentItem(entity.unwrap())

    override fun fall(distance: Float, damageMultiplier: Float) = wrapped.fall(distance, damageMultiplier)

    override fun triggerAchievement(stat: IStatBase) = wrapped.triggerAchievement(stat.unwrap())

    override fun jump() = wrapped.jump()

    override fun getCooledAttackStrength(fl: Float): Float = Backend.BACKEND_UNSUPPORTED()

    override fun resetCooldown() = Backend.BACKEND_UNSUPPORTED()

}

inline fun IEntityPlayer.unwrap(): EntityPlayer = (this as EntityPlayerImpl<*>).wrapped
inline fun EntityPlayer.wrap(): IEntityPlayer = EntityPlayerImpl(this)
