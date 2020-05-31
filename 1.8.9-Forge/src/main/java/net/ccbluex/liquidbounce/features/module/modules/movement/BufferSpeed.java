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
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockSlab
import net.minecraft.block.BlockSlime
import net.minecraft.block.BlockStairs
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
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
        if (LiquidBounce.moduleManager.getModule(Speed::class.java)!!.state || noHurtValue.get() && mc.thePlayer.hurtTime > 0) {
            reset()
            return
        }
        val blockPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY,
                mc.thePlayer.posZ)
        if (forceDown || down && mc.thePlayer.motionY == 0.0) {
            mc.thePlayer.motionY = -1.0
            down = false
            forceDown = false
        }
        if (fastHop) {
            mc.thePlayer.speedInAir = 0.0211f
            hadFastHop = true
        } else if (hadFastHop) {
            mc.thePlayer.speedInAir = 0.02f
            hadFastHop = false
        }
        if (!MovementUtils.isMoving() || mc.thePlayer.isSneaking || mc.thePlayer.isInWater ||
                mc.gameSettings.keyBindJump.isKeyDown) {
            reset()
            return
        }
        if (mc.thePlayer.onGround) {
            fastHop = false
            if (slimeValue.get() && (getBlock(blockPos.down()) is BlockSlime ||
                            getBlock(blockPos) is BlockSlime)) {
                mc.thePlayer.jump()
                mc.thePlayer.motionY = 0.08
                mc.thePlayer.motionX *= 1.132
                mc.thePlayer.motionZ *= 1.132
                down = true
                return
            }
            if (slabsValue.get() && getBlock(blockPos) is BlockSlab) {
                when (slabsModeValue.get().toLowerCase()) {
                    "old" -> {
                        boost(slabsBoostValue.get())
                        return
                    }
                    "new" -> {
                        fastHop = true
                        if (legitHop) {
                            mc.thePlayer.jump()
                            mc.thePlayer.onGround = false
                            legitHop = false
                            return
                        }
                        mc.thePlayer.onGround = false
                        MovementUtils.strafe(0.375f)
                        mc.thePlayer.jump()
                        mc.thePlayer.motionY = 0.41
                        return
                    }
                }
            }
            if (stairsValue.get() && (getBlock(blockPos.down()) is BlockStairs ||
                            getBlock(blockPos) is BlockStairs)) {
                when (stairsModeValue.get().toLowerCase()) {
                    "old" -> {
                        boost(stairsBoostValue.get())
                        return
                    }
                    "new" -> {
                        fastHop = true
                        if (legitHop) {
                            mc.thePlayer.jump()
                            mc.thePlayer.onGround = false
                            legitHop = false
                            return
                        }
                        mc.thePlayer.onGround = false
                        MovementUtils.strafe(0.375f)
                        mc.thePlayer.jump()
                        mc.thePlayer.motionY = 0.41
                        return
                    }
                }
            }
            legitHop = true
            if (headBlockValue.get() && getBlock(blockPos.up(2)) !== Blocks.air) {
                boost(headBlockBoostValue.get())
                return
            }
            if (iceValue.get() && (getBlock(blockPos.down()) === Blocks.ice ||
                            getBlock(blockPos.down()) === Blocks.packed_ice)) {
                boost(iceBoostValue.get())
                return
            }
            if (snowValue.get() && getBlock(blockPos) === Blocks.snow_layer &&
                    (snowPortValue.get() || mc.thePlayer.posY - mc.thePlayer.posY.toInt() >= 0.12500)) {
                if (mc.thePlayer.posY - mc.thePlayer.posY.toInt() >= 0.12500) boost(snowBoostValue.get()) else {
                    mc.thePlayer.jump()
                    forceDown = true
                }
                return
            }
            if (wallValue.get()) {
                when (wallModeValue.get().toLowerCase()) {
                    "old" -> if (mc.thePlayer.isCollidedHorizontally && isNearBlock || getBlock(BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2.0, mc.thePlayer.posZ)) !is BlockAir) {
                        boost(wallBoostValue.get())
                        return
                    }
                    "new" -> if (isNearBlock && !mc.thePlayer.movementInput.jump) {
                        mc.thePlayer.jump()
                        mc.thePlayer.motionY = 0.08
                        mc.thePlayer.motionX *= 0.99
                        mc.thePlayer.motionZ *= 0.99
                        down = true
                        return
                    }
                }
            }
            val currentSpeed = MovementUtils.getSpeed()
            if (speed < currentSpeed) speed = currentSpeed.toDouble()
            if (bufferValue.get() && speed > 0.2f) {
                speed /= 1.0199999809265137
                MovementUtils.strafe(speed.toFloat())
            }
        } else {
            speed = 0.0
            if (airStrafeValue.get()) MovementUtils.strafe()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) speed = 0.0
    }

    override fun onEnable() {
        reset()
    }

    override fun onDisable() {
        reset()
    }

    private fun reset() {
        if (mc.thePlayer == null) return
        legitHop = true
        speed = 0.0
        if (hadFastHop) {
            mc.thePlayer.speedInAir = 0.02f
            hadFastHop = false
        }
    }

    private fun boost(boost: Float) {
        mc.thePlayer.motionX *= boost.toDouble()
        mc.thePlayer.motionZ *= boost.toDouble()
        speed = MovementUtils.getSpeed().toDouble()
        if (speedLimitValue.get() && speed > maxSpeedValue.get()) speed = maxSpeedValue.get().toDouble()
    }

    private val isNearBlock: Boolean
        get() {
            val thePlayer = mc.thePlayer
            val theWorld = mc.theWorld
            val blocks: MutableList<BlockPos> = ArrayList()
            blocks.add(BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ - 0.7))
            blocks.add(BlockPos(thePlayer.posX + 0.7, thePlayer.posY + 1, thePlayer.posZ))
            blocks.add(BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ + 0.7))
            blocks.add(BlockPos(thePlayer.posX - 0.7, thePlayer.posY + 1, thePlayer.posZ))
            for (blockPos in blocks) if (theWorld.getBlockState(blockPos).block.blockBoundsMaxY ==
                    theWorld.getBlockState(blockPos).block.blockBoundsMinY + 1 &&
                    !theWorld.getBlockState(blockPos).block.isTranslucent && theWorld.getBlockState(blockPos).block !== Blocks.water &&
                    theWorld.getBlockState(blockPos).block !is BlockSlab ||
                    theWorld.getBlockState(blockPos).block === Blocks.barrier) return true
            return false
        }
}
