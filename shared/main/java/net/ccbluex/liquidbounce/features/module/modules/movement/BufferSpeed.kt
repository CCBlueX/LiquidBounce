/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import java.util.*

@ModuleInfo(name = "BufferSpeed", description = "Allows you to walk faster on slabs and stairs.", category = ModuleCategory.MOVEMENT)
class BufferSpeed : Module() {
    private val speedLimitValue = BoolValue("SpeedLimit", true)
    private val maxSpeedValue = FloatValue("MaxSpeed", 2.0f, 1.0f, 5f)
    private val bufferValue = BoolValue("Buffer", true)

    private val stairsValue = BoolValue("Stairs", true)
    private val stairsBoostValue = FloatValue("StairsBoost", 1.87f, 1f, 2f)
    private val stairsModeValue = ListValue("StairsMode", arrayOf("Old", "New"), "New")
    private val slabsValue = BoolValue("Slabs", true)
    private val slabsBoostValue = FloatValue("SlabsBoost", 1.87f, 1f, 2f)
    private val slabsModeValue = ListValue("SlabsMode", arrayOf("Old", "New"), "New")
    private val iceValue = BoolValue("Ice", false)
    private val iceBoostValue = FloatValue("IceBoost", 1.342f, 1f, 2f)
    private val snowValue = BoolValue("Snow", true)
    private val snowBoostValue = FloatValue("SnowBoost", 1.87f, 1f, 2f)
    private val snowPortValue = BoolValue("SnowPort", true)
    private val wallValue = BoolValue("Wall", true)
    private val wallBoostValue = FloatValue("WallBoost", 1.87f, 1f, 2f)
    private val wallModeValue = ListValue("WallMode", arrayOf("Old", "New"), "New")
    private val headBlockValue = BoolValue("HeadBlock", true)
    private val headBlockBoostValue = FloatValue("HeadBlockBoost", 1.87f, 1f, 2f)
    private val slimeValue = BoolValue("Slime", true)
    private val airStrafeValue = BoolValue("AirStrafe", false)
    private val noHurtValue = BoolValue("NoHurt", true)

    private var speed = 0.0
    private var down = false
    private var forceDown = false
    private var fastHop = false
    private var hadFastHop = false
    private var legitHop = false

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        val thePlayer = mc.thePlayer ?: return

        if (LiquidBounce.moduleManager.getModule(Speed::class.java)!!.state || noHurtValue.get() && thePlayer.hurtTime > 0) {
            reset()
            return
        }

        val blockPos = WBlockPos(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ)

        if (forceDown || down && thePlayer.motionY == 0.0) {
            thePlayer.motionY = -1.0
            down = false
            forceDown = false
        }

        if (fastHop) {
            thePlayer.speedInAir = 0.0211f
            hadFastHop = true
        } else if (hadFastHop) {
            thePlayer.speedInAir = 0.02f
            hadFastHop = false
        }

        if (!MovementUtils.isMoving || thePlayer.sneaking || thePlayer.isInWater || mc.gameSettings.keyBindJump.isKeyDown) {
            reset()
            return
        }

        if (thePlayer.onGround) {
            fastHop = false

            if (slimeValue.get() && (classProvider.isBlockSlime(getBlock(blockPos.down())) || classProvider.isBlockSlime(getBlock(blockPos)))) {
                thePlayer.jump()

                thePlayer.motionX = thePlayer.motionY * 1.132
                thePlayer.motionY = 0.08
                thePlayer.motionZ = thePlayer.motionY * 1.132

                down = true
                return
            }
            if (slabsValue.get() && classProvider.isBlockSlab(getBlock(blockPos))) {
                when (slabsModeValue.get().toLowerCase()) {
                    "old" -> {
                        boost(slabsBoostValue.get())
                        return
                    }
                    "new" -> {
                        fastHop = true
                        if (legitHop) {
                            thePlayer.jump()
                            thePlayer.onGround = false
                            legitHop = false
                            return
                        }
                        thePlayer.onGround = false

                        MovementUtils.strafe(0.375f)

                        thePlayer.jump()
                        thePlayer.motionY = 0.41
                        return
                    }
                }
            }
            if (stairsValue.get() && (classProvider.isBlockStairs(getBlock(blockPos.down())) || classProvider.isBlockStairs(getBlock(blockPos)))) {
                when (stairsModeValue.get().toLowerCase()) {
                    "old" -> {
                        boost(stairsBoostValue.get())
                        return
                    }
                    "new" -> {
                        fastHop = true

                        if (legitHop) {
                            thePlayer.jump()
                            thePlayer.onGround = false
                            legitHop = false
                            return
                        }

                        thePlayer.onGround = false
                        MovementUtils.strafe(0.375f)
                        thePlayer.jump()
                        thePlayer.motionY = 0.41
                        return
                    }
                }
            }
            legitHop = true

            if (headBlockValue.get() && getBlock(blockPos.up(2)) == classProvider.getBlockEnum(BlockType.AIR)) {
                boost(headBlockBoostValue.get())
                return
            }

            if (iceValue.get() && (getBlock(blockPos.down()) == classProvider.getBlockEnum(BlockType.ICE) || getBlock(blockPos.down()) == classProvider.getBlockEnum(BlockType.ICE_PACKED))) {
                boost(iceBoostValue.get())
                return
            }

            if (snowValue.get() && getBlock(blockPos) == classProvider.getBlockEnum(BlockType.SNOW_LAYER) && (snowPortValue.get() || thePlayer.posY - thePlayer.posY.toInt() >= 0.12500)) {
                if (thePlayer.posY - thePlayer.posY.toInt() >= 0.12500) {
                    boost(snowBoostValue.get())
                } else {
                    thePlayer.jump()
                    forceDown = true
                }
                return
            }

            if (wallValue.get()) {
                when (wallModeValue.get().toLowerCase()) {
                    "old" -> if (thePlayer.isCollidedVertically && isNearBlock || !classProvider.isBlockAir(getBlock(WBlockPos(thePlayer.posX, thePlayer.posY + 2.0, thePlayer.posZ)))) {
                        boost(wallBoostValue.get())
                        return
                    }
                    "new" ->
                        if (isNearBlock && !thePlayer.movementInput.jump) {
                            thePlayer.jump()
                            thePlayer.motionY = 0.08
                            thePlayer.motionX = thePlayer.motionX * 0.99
                            thePlayer.motionZ = thePlayer.motionX * 0.99
                            down = true
                            return
                        }
                }
            }
            val currentSpeed = MovementUtils.speed

            if (speed < currentSpeed)
                speed = currentSpeed.toDouble()

            if (bufferValue.get() && speed > 0.2f) {
                speed /= 1.0199999809265137
                MovementUtils.strafe(speed.toFloat())
            }
        } else {
            speed = 0.0

            if (airStrafeValue.get())
                MovementUtils.strafe()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (classProvider.isSPacketPlayerPosLook(packet))
            speed = 0.0
    }

    override fun onEnable() {
        reset()
    }

    override fun onDisable() {
        reset()
    }

    private fun reset() {
        val thePlayer = mc.thePlayer ?: return
        legitHop = true
        speed = 0.0

        if (hadFastHop) {
            thePlayer.speedInAir = 0.02f
            hadFastHop = false
        }
    }

    private inline fun boost(boost: Float) {
        val thePlayer = mc.thePlayer!!

        thePlayer.motionX = thePlayer.motionX * boost
        thePlayer.motionZ = thePlayer.motionX * boost

        speed = MovementUtils.speed.toDouble()

        if (speedLimitValue.get() && speed > maxSpeedValue.get())
            speed = maxSpeedValue.get().toDouble()
    }

    private val isNearBlock: Boolean
        get() {
            val thePlayer = mc.thePlayer
            val theWorld = mc.theWorld
            val blocks: MutableList<WBlockPos> = ArrayList()
            blocks.add(WBlockPos(thePlayer!!.posX, thePlayer.posY + 1, thePlayer.posZ - 0.7))
            blocks.add(WBlockPos(thePlayer.posX + 0.7, thePlayer.posY + 1, thePlayer.posZ))
            blocks.add(WBlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ + 0.7))
            blocks.add(WBlockPos(thePlayer.posX - 0.7, thePlayer.posY + 1, thePlayer.posZ))
            for (blockPos in blocks) {
                val blockState = theWorld!!.getBlockState(blockPos)

                val collisionBoundingBox = blockState.block.getCollisionBoundingBox(theWorld, blockPos, blockState)

                if ((collisionBoundingBox == null || collisionBoundingBox.maxX ==
                                collisionBoundingBox.minY + 1) &&
                        !blockState.block.isTranslucent(blockState) && blockState.block == classProvider.getBlockEnum(BlockType.WATER) &&
                        !classProvider.isBlockSlab(blockState.block) || blockState.block == classProvider.getBlockEnum(BlockType.BARRIER)) return true
            }
            return false
        }
}