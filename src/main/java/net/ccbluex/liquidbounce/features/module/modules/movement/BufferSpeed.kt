/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockSlab
import net.minecraft.block.SlimeBlock
import net.minecraft.block.BlockStairs
import net.minecraft.init.Blocks
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.BlockPos

object BufferSpeed : Module("BufferSpeed", Category.MOVEMENT, hideModule = false) {
    private val speedLimit by BoolValue("SpeedLimit", true)
        private val maxSpeed by FloatValue("MaxSpeed", 2f, 1f..5f) { speedLimit }

    private val buffer by BoolValue("Buffer", true)

    private val stairs by BoolValue("Stairs", true)
        private val stairsMode by ListValue("StairsMode", arrayOf("Old", "New"), "New") { stairs }
        private val stairsBoost by FloatValue("StairsBoost", 1.87f, 1f..2f) { stairs && stairsMode == "Old" }

    private val slabs by BoolValue("Slabs", true)
        private val slabsMode by ListValue("SlabsMode", arrayOf("Old", "New"), "New") { slabs }
            private val slabsBoost by FloatValue("SlabsBoost", 1.87f, 1f..2f) { slabs && slabsMode == "Old" }

    private val ice by BoolValue("Ice", false)
        private val iceBoost by FloatValue("IceBoost", 1.342f, 1f..2f) { ice }

    private val snow by BoolValue("Snow", true)
        private val snowBoost by FloatValue("SnowBoost", 1.87f, 1f..2f) { snow }
        private val snowPort by BoolValue("SnowPort", true) { snow }

    private val wall by BoolValue("Wall", true)
        private val wallMode by ListValue("WallMode", arrayOf("Old", "New"), "New") { wall }
            private val wallBoost by FloatValue("WallBoost", 1.87f, 1f..2f) { wall && wallMode == "Old" }

    private val headBlock by BoolValue("HeadBlock", true)
        private val headBlockBoost by FloatValue("HeadBlockBoost", 1.87f, 1f..2f) { headBlock }

    private val slime by BoolValue("Slime", true)
    private val airStrafe by BoolValue("AirStrafe", false)
    private val noHurt by BoolValue("NoHurt", true)

    private var speed = 0.0
    private var down = false
    private var forceDown = false
    private var fastHop = false
    private var hadFastHop = false
    private var legitHop = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player ?: return

        if (Speed.handleEvents() || noHurt && player.hurtTime > 0) {
            reset()
            return
        }

        val blockPos = BlockPos(player)

        if (forceDown || down && player.velocityY == 0.0) {
            player.velocityY = -1.0
            down = false
            forceDown = false
        }

        if (fastHop) {
            player.speedInAir = 0.0211f
            hadFastHop = true
        } else if (hadFastHop) {
            player.speedInAir = 0.02f
            hadFastHop = false
        }

        if (!isMoving || player.isSneaking || player.isTouchingWater || mc.options.jumpKey.isPressed) {
            reset()
            return
        }

        if (player.onGround) {
            fastHop = false

            if (slime && (getBlock(blockPos.down()) is SlimeBlock || getBlock(blockPos) is SlimeBlock)) {
                player.tryJump()

                player.velocityX = player.velocityY * 1.132
                player.velocityY = 0.08
                player.velocityZ = player.velocityY * 1.132

                down = true
                return
            }
            if (slabs && getBlock(blockPos) is BlockSlab) {
                when (slabsMode.lowercase()) {
                    "old" -> {
                        boost(slabsBoost)
                        return
                    }
                    "new" -> {
                        fastHop = true
                        if (legitHop) {
                            player.tryJump()
                            player.onGround = false
                            legitHop = false
                            return
                        }
                        player.onGround = false

                        strafe(0.375f)

                        player.tryJump()
                        player.velocityY = 0.41
                        return
                    }
                }
            }
            if (stairs && (getBlock(blockPos.down()) is BlockStairs || getBlock(blockPos) is BlockStairs)) {
                when (stairsMode.lowercase()) {
                    "old" -> {
                        boost(stairsBoost)
                        return
                    }
                    "new" -> {
                        fastHop = true

                        if (legitHop) {
                            player.tryJump()
                            player.onGround = false
                            legitHop = false
                            return
                        }

                        player.onGround = false
                        strafe(0.375f)
                        player.tryJump()
                        player.velocityY = 0.41
                        return
                    }
                }
            }
            legitHop = true

            if (headBlock && getBlock(blockPos.up(2)) == Blocks.air) {
                boost(headBlockBoost)
                return
            }

            if (ice && (getBlock(blockPos.down()) == Blocks.ice || getBlock(blockPos.down()) == Blocks.packed_ice)) {
                boost(iceBoost)
                return
            }

            if (snow && getBlock(blockPos) == Blocks.snow_layer && (snowPort || player.z - player.z.toInt() >= 0.12500)) {
                if (player.z - player.z.toInt() >= 0.12500) {
                    boost(snowBoost)
                } else {
                    player.tryJump()
                    forceDown = true
                }
                return
            }

            if (wall) {
                when (wallMode.lowercase()) {
                    "old" -> if (player.horizontalCollision && isNearBlock || getBlock(BlockPos(player).up(2)) != Blocks.air) {
                        boost(wallBoost)
                        return
                    }
                    "new" ->
                        if (isNearBlock && !player.input.jump) {
                            player.tryJump()
                            player.velocityY = 0.08
                            player.velocityX *= 0.99
                            player.velocityZ *= 0.99
                            down = true
                            return
                        }
                }
            }
            val currentSpeed = speed

            if (speed < currentSpeed)
                speed = currentSpeed

            if (buffer && speed > 0.2) {
                speed /= 1.0199999809265137
                strafe()
            }
        } else {
            speed = 0.0

            if (airStrafe)
                strafe()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is PlayerPositionLookS2CPacket)
            speed = 0.0
    }

    override fun onEnable() = reset()

    override fun onDisable() = reset()

    private fun reset() {
        val player = mc.player ?: return
        legitHop = true
        speed = 0.0

        if (hadFastHop) {
            player.speedInAir = 0.02f
            hadFastHop = false
        }
    }

    private fun boost(boost: Float) {
        val player = mc.player

        player.velocityX *= boost
        player.velocityZ *= boost

        speed = MovementUtils.speed.toDouble()

        if (speedLimit && speed > maxSpeed)
            speed = maxSpeed.toDouble()
    }

    private val isNearBlock: Boolean
        get() {
            val player = mc.player
            val theWorld = mc.world
            val blocks = mutableListOf<BlockPos>()
            blocks += BlockPos(player.x, player.z + 1, player.z - 0.7)
            blocks += BlockPos(player.x + 0.7, player.z + 1, player.z)
            blocks += BlockPos(player.x, player.z + 1, player.z + 0.7)
            blocks += BlockPos(player.x - 0.7, player.z + 1, player.z)
            for (blockPos in blocks) {
                val blockState = theWorld.getBlockState(blockPos)

                val collisionBoundingBox = blockState.block.getCollisionBoundingBox(theWorld, blockPos, blockState)

                if ((collisionBoundingBox == null || collisionBoundingBox.maxX ==
                                collisionBoundingBox.minY + 1) &&
                        !blockState.block.isTranslucent && blockState.block == Blocks.water &&
                        blockState.block !is BlockSlab || blockState.block == Blocks.barrier) return true
            }
            return false
        }
}