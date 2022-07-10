/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.multiply
import net.ccbluex.liquidbounce.utils.extensions.speed
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.ValueGroup
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockSlab
import net.minecraft.block.BlockSlime
import net.minecraft.block.BlockStairs
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.world.World
import java.util.*

@ModuleInfo(name = "BufferSpeed", description = "Allows you to walk faster on slabs, stairs, ice and more. (a.k.a. TerrainSpeed)", category = ModuleCategory.MOVEMENT)
class BufferSpeed : Module()
{
    private val speedLimitValue = BoolValue("SpeedLimit", true)
    private val maxSpeedValue = FloatValue("MaxSpeed", 2.0f, 1.0f, 5f)
    private val bufferValue = BoolValue("Buffer", true)

    private val stairsGroup = ValueGroup("Stairs")
    private val stairsEnabledValue = BoolValue("Enabled", true, "Stairs")
    private val stairsModeValue = ListValue("Mode", arrayOf("Old", "New"), "New", "StairsMode")
    private val stairsBoostValue = FloatValue("Boost", 1.87f, 1f, 2f, "StairsBoost")

    private val slabsGroup = ValueGroup("Slabs")
    private val slabsEnabledValue = BoolValue("Enabled", true, "Slabs")
    private val slabsModeValue = ListValue("Mode", arrayOf("Old", "New"), "New", "SlabsMode")
    private val slabsBoostValue = FloatValue("Boost", 1.87f, 1f, 2f, "SlabsBoost")

    private val iceGroup = ValueGroup("Ice")
    private val iceEnabledValue = BoolValue("Enabled", false, "Ice")
    private val iceBoostValue = FloatValue("Boost", 1.342f, 1f, 2f, "IceBoost")

    private val snowGroup = ValueGroup("Snow") // AAC3.3.6 SnowSpeed
    private val snowEnabledValue = BoolValue("Enabled", true, "Snow")
    private val snowBoostValue = FloatValue("Boost", 1.87f, 1f, 2f, "SnowBoost")
    private val snowPortValue = BoolValue("Port", true, "SnowPort")

    private val wallGroup = ValueGroup("Wall")
    private val wallEnabledValue = BoolValue("Enabled", true, "Wall")
    private val wallModeValue = ListValue("Mode", arrayOf("AAC3.2.1", "AAC3.3.8"), "AAC3.3.8", "WallMode")
    private val wallBoostValue = FloatValue("Boost", 1.87f, 1f, 2f, "WallBoost")

    private val headBlockGroup = ValueGroup("HeadBlock")
    private val headBlockEnabledValue = BoolValue("Enabled", true, "HeadBlock")
    private val headBlockBoostValue = FloatValue("Boost", 1.87f, 1f, 2f, "HeadBlockBoost")

    private val slimeValue = BoolValue("Slime", true)

    private val airStrafeValue = BoolValue("AirStrafe", false)
    private val noHurtValue = BoolValue("NoHurt", true)

    private var speed = 0.0F
    private var down = false
    private var forceDown = false
    private var fastHop = false
    private var hadFastHop = false
    private var legitHop = false

    init
    {
        stairsGroup.addAll(stairsEnabledValue, stairsModeValue, stairsBoostValue)
        slabsGroup.addAll(slabsEnabledValue, slabsModeValue, slabsBoostValue)
        iceGroup.addAll(iceEnabledValue, iceBoostValue)
        snowGroup.addAll(snowEnabledValue, snowBoostValue, snowPortValue)
        wallGroup.addAll(wallEnabledValue, wallModeValue, wallBoostValue)
        headBlockGroup.addAll(headBlockEnabledValue, headBlockBoostValue)
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (LiquidBounce.moduleManager[Speed::class.java].state || noHurtValue.get() && thePlayer.hurtTime > 0)
        {
            reset()
            return
        }

        val blockPos = BlockPos(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ)

        if (forceDown || down && thePlayer.motionY == 0.0)
        {
            thePlayer.motionY = -1.0
            down = false
            forceDown = false
        }

        if (fastHop)
        {
            thePlayer.speedInAir = 0.0211f
            hadFastHop = true
        }
        else if (hadFastHop)
        {
            thePlayer.speedInAir = 0.02f
            hadFastHop = false
        }

        if (!thePlayer.isMoving || thePlayer.isSneaking || thePlayer.isInWater || mc.gameSettings.keyBindJump.isKeyDown)
        {
            reset()
            return
        }

        if (thePlayer.onGround)
        {
            fastHop = false

            if (slimeValue.get() && (theWorld.getBlock(blockPos.down()) is BlockSlime || theWorld.getBlock(blockPos) is BlockSlime))
            {
                thePlayer.jump()

                thePlayer.multiply(1.132)
                thePlayer.motionY = 0.08

                down = true
                return
            }

            if (slabsEnabledValue.get() && theWorld.getBlock(blockPos) is BlockSlab)
            {
                when (slabsModeValue.get().toLowerCase())
                {
                    "old" ->
                    {
                        boost(thePlayer, slabsBoostValue.get())
                        return
                    }

                    "new" ->
                    {
                        fastHop = true
                        if (legitHop)
                        {
                            thePlayer.jump()
                            thePlayer.onGround = false
                            legitHop = false
                            return
                        }
                        thePlayer.onGround = false

                        thePlayer.strafe(0.375f)

                        thePlayer.jump()
                        thePlayer.motionY = 0.41
                        return
                    }
                }
            }

            if (stairsEnabledValue.get() && (theWorld.getBlock(blockPos.down()) is BlockStairs || theWorld.getBlock(blockPos) is BlockStairs))
            {
                when (stairsModeValue.get().toLowerCase())
                {
                    "old" ->
                    {
                        boost(thePlayer, stairsBoostValue.get())
                        return
                    }

                    "new" ->
                    {
                        fastHop = true

                        if (legitHop)
                        {
                            thePlayer.jump()
                            thePlayer.onGround = false
                            legitHop = false
                            return
                        }

                        thePlayer.onGround = false
                        thePlayer.strafe(0.375f)
                        thePlayer.jump()
                        thePlayer.motionY = 0.41
                        return
                    }
                }
            }
            legitHop = true

            if (headBlockEnabledValue.get() && theWorld.getBlock(blockPos.up(2)) != Blocks.air)
            {
                boost(thePlayer, headBlockBoostValue.get())
                return
            }

            if (iceEnabledValue.get() && (theWorld.getBlock(blockPos.down()) == Blocks.ice || theWorld.getBlock(blockPos.down()) == Blocks.packed_ice))
            {
                boost(thePlayer, iceBoostValue.get())
                return
            }

            if (snowEnabledValue.get() && theWorld.getBlock(blockPos) == Blocks.snow_layer && (snowPortValue.get() || thePlayer.posY - thePlayer.posY.toInt() >= 0.12500))
            {
                if (thePlayer.posY - thePlayer.posY.toInt() >= 0.12500) boost(thePlayer, snowBoostValue.get())
                else
                {
                    thePlayer.jump()
                    forceDown = true
                }
                return
            }

            if (wallEnabledValue.get())
            {
                when (wallModeValue.get().toLowerCase())
                {
                    "aac3.2.1" -> if (thePlayer.isCollidedVertically && isNearBlock(theWorld, thePlayer) || theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY + 2.0, thePlayer.posZ)) !is BlockAir)
                    {
                        boost(thePlayer, wallBoostValue.get())

                        return
                    }

                    "aac3.3.8" -> if (isNearBlock(theWorld, thePlayer) && !thePlayer.movementInput.jump)
                    {
                        thePlayer.jump()

                        thePlayer.multiply(0.99)
                        thePlayer.motionY = 0.08

                        down = true

                        return
                    }
                }
            }

            val currentSpeed = thePlayer.speed

            if (speed < currentSpeed) speed = currentSpeed

            if (bufferValue.get() && speed > 0.2f)
            {
                speed /= 1.0199999809265137F
                thePlayer.strafe(speed)
            }
        }
        else
        {
            speed = 0.0F

            if (airStrafeValue.get()) thePlayer.strafe()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) speed = 0.0F
    }

    override fun onEnable()
    {
        reset()
    }

    override fun onDisable()
    {
        reset()
    }

    private fun reset()
    {
        val thePlayer = mc.thePlayer ?: return
        legitHop = true
        speed = 0.0F

        if (hadFastHop)
        {
            thePlayer.speedInAir = 0.02f
            hadFastHop = false
        }
    }

    private fun boost(thePlayer: Entity, boost: Float)
    {
        thePlayer.multiply(boost)
        speed = thePlayer.speed

        val maxSpeed = maxSpeedValue.get()
        if (speedLimitValue.get() && speed > maxSpeed) speed = maxSpeed
    }

    private fun isNearBlock(theWorld: World, thePlayer: Entity): Boolean
    {
        val blocks = ArrayDeque<BlockPos>(4)

        blocks.add(BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ - 0.7))
        blocks.add(BlockPos(thePlayer.posX + 0.7, thePlayer.posY + 1, thePlayer.posZ))
        blocks.add(BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ + 0.7))
        blocks.add(BlockPos(thePlayer.posX - 0.7, thePlayer.posY + 1, thePlayer.posZ))

        return blocks.map { blockPos ->
            val blockState = theWorld.getBlockState(blockPos)
            blockState to blockState.block.getCollisionBoundingBox(theWorld, blockPos, blockState)
        }.any { (blockState, collisionBoundingBox) ->
            val block = blockState.block
            block == Blocks.barrier || (collisionBoundingBox == null || collisionBoundingBox.maxX == collisionBoundingBox.minY + 1) && !block.isTranslucent && block == Blocks.water && block !is BlockSlab
        }
    }
}
