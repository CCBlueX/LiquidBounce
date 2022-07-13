package net.ccbluex.liquidbounce.features.module.modules.movement.flies

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.exploit.Damage
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.boost
import net.ccbluex.liquidbounce.utils.extensions.getBlockCollisionBox
import net.ccbluex.liquidbounce.utils.extensions.getEffectAmplifier
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.minecraft.block.BlockAir
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.potion.Potion
import net.minecraft.stats.StatList
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.world.World
import kotlin.math.min

abstract class FlyMode(val modeName: String) : MinecraftInstance()
{
    open val shouldDisableNoFall: Boolean
        get() = false

    open val mark: Boolean
        get() = true

    open val damageOnStart: DamageOnStart
        get() = DamageOnStart.OFF

    abstract fun onUpdate()

    open fun onMotion(eventState: EventState)
    {
    }

    open fun onRender3D(partialTicks: Float)
    {
    }

    open fun onPacket(event: PacketEvent)
    {
    }

    open fun onMove(event: MoveEvent)
    {
    }

    open fun onBlockBB(event: BlockBBEvent)
    {
    }

    open fun onJump(event: JumpEvent)
    {
    }

    open fun onStep(event: StepEvent)
    {
    }

    open fun onEnable()
    {
    }

    open fun onDisable()
    {
    }

    companion object
    {
        fun handleVanillaKickBypass(theWorld: World, thePlayer: Entity)
        {
            if (!Fly.vanillaKickBypassValue.get() || !Fly.groundTimer.hasTimePassed(1000)) return

            val networkManager = mc.netHandler.networkManager
            val ground = calculateGround(theWorld, thePlayer)

            val posX = thePlayer.posX
            val originalPosY = thePlayer.posY
            val posZ = thePlayer.posZ

            run {
                var posY = originalPosY
                while (posY > ground)
                {
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(posX, posY, posZ, true))
                    if (posY - 8.0 < ground) break // Prevent next step
                    posY -= 8.0
                }
            }

            networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(posX, ground, posZ, true))

            var posY = ground
            while (posY < originalPosY)
            {
                networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(posX, posY, posZ, true))
                if (posY + 8.0 > originalPosY) break // Prevent next step
                posY += 8.0
            }

            networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(posX, originalPosY, posZ, true))

            Fly.groundTimer.reset()
        }

        // TODO: Make better and faster calculation lol
        private fun calculateGround(theWorld: World, thePlayer: Entity): Double
        {
            val playerBoundingBox: AxisAlignedBB = thePlayer.entityBoundingBox
            var blockHeight = 1.0
            var ground = thePlayer.posY
            while (ground > 0.0)
            {
                val customBox = AxisAlignedBB(playerBoundingBox.maxX, ground + blockHeight, playerBoundingBox.maxZ, playerBoundingBox.minX, ground, playerBoundingBox.minZ)
                if (theWorld.checkBlockCollision(customBox))
                {
                    if (blockHeight <= 0.05) return ground + blockHeight

                    ground += blockHeight
                    blockHeight = 0.05
                }
                ground -= blockHeight
            }
            return 0.0
        }

        fun jump(theWorld: World, thePlayer: EntityPlayer)
        {
            val blockAboveState = theWorld.getBlockState(BlockPos(thePlayer.posX, thePlayer.posY + 2, thePlayer.posZ))
            val blockAbove = blockAboveState.block
            val normalJumpY = 0.42 + thePlayer.getEffectAmplifier(Potion.jump) * 0.1f
            val jumpY = if (blockAbove is BlockAir) normalJumpY else min(blockAboveState.let { theWorld.getBlockCollisionBox(it)?.minY?.plus(0.2) } ?: normalJumpY, normalJumpY)

            // Simulate Vanilla Player Jump
            thePlayer.setPosition(thePlayer.posX, thePlayer.posY + jumpY, thePlayer.posZ)

            // Jump Boost
            if (thePlayer.isSprinting) thePlayer.boost(0.2f)
            thePlayer.isAirBorne = true

            // ForgeHooks.onLivingJump(thePlayer)
            thePlayer.triggerAchievement(StatList.jumpStat)
        }
    }
}

enum class DamageOnStart(val execute: () -> Unit)
{
    OFF({ }),
    NONE({ }), // Used internally
    NCP(Damage::ncpDamage),
    OLD_NCP({ net.ccbluex.liquidbounce.features.module.modules.exploit.Damage.ncpDamage(motionSize = 1.01) }),
    HYPIXEL(Damage::hypixelDamage);

    companion object
    {
        fun byName(name: String): DamageOnStart? = values().find { it.name.equals(name, ignoreCase = true) }
    }
}
