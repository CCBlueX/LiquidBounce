/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.MinecraftVersion
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isAnimal
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isMob
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*
import kotlin.math.*

@ModuleInfo(name = "KillAura", description = "Automatically attacks targets around you.",
        category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_R)
class KillAura : Module() {

    /**
     * OPTIONS
     */

    // CPS - Attack speed
    private val maxCPS: IntegerValue = object : IntegerValue("MaxCPS", 8, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minCPS.get()
            if (i > newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), this.get())
        }
    }

    private val minCPS: IntegerValue = object : IntegerValue("MinCPS", 5, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxCPS.get()
            if (i < newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(this.get(), maxCPS.get())
        }
    }

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
    private val cooldownValue = FloatValue("Cooldown", 1f, 0f, 1f)

    // Range
    private val rangeValue = FloatValue("Range", 3.7f, 1f, 8f)
    private val throughWallsRangeValue = FloatValue("ThroughWallsRange", 3f, 0f, 8f)
    private val rangeSprintReducementValue = FloatValue("RangeSprintReducement", 0f, 0f, 0.4f)

    // Modes
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "Direction", "LivingTime"), "Distance")
    private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")

    // Bypass
    private val swingValue = BoolValue("Swing", true)
    private val keepSprintValue = BoolValue("KeepSprint", true)

    // AutoBlock
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Packet", "AfterTick"), "Packet")
    private val interactAutoBlockValue = BoolValue("InteractAutoBlock", true)
    private val blockRate = IntegerValue("BlockRate", 100, 1, 100)

    // Raycast
    private val raycastValue = BoolValue("RayCast", true)
    private val raycastIgnoredValue = BoolValue("RayCastIgnored", false)
    private val livingRaycastValue = BoolValue("LivingRayCast", true)

    // Bypass
    private val aacValue = BoolValue("AAC", false)

    // Turn Speed
    private val maxTurnSpeed: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 0f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minTurnSpeed.get()
            if (v > newValue) set(v)
        }
    }

    private val minTurnSpeed: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 0f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxTurnSpeed.get()
            if (v < newValue) set(v)
        }
    }

    private val silentRotationValue = BoolValue("SilentRotation", true)
    private val rotationStrafeValue = ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off")
    private val randomCenterValue = BoolValue("RandomCenter", true)
    private val outborderValue = BoolValue("Outborder", false)
    private val fovValue = FloatValue("FOV", 180f, 0f, 180f)

    // Predict
    private val predictValue = BoolValue("Predict", true)

    private val maxPredictSize: FloatValue = object : FloatValue("MaxPredictSize", 1f, 0.1f, 5f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minPredictSize.get()
            if (v > newValue) set(v)
        }
    }

    private val minPredictSize: FloatValue = object : FloatValue("MinPredictSize", 1f, 0.1f, 5f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxPredictSize.get()
            if (v < newValue) set(v)
        }
    }

    // Bypass
    private val failRateValue = FloatValue("FailRate", 0f, 0f, 100f)
    private val fakeSwingValue = BoolValue("FakeSwing", true)
    private val noInventoryAttackValue = BoolValue("NoInvAttack", false)
    private val noInventoryDelayValue = IntegerValue("NoInvDelay", 200, 0, 500)
    private val limitedMultiTargetsValue = IntegerValue("LimitedMultiTargets", 0, 0, 50)

    // Visuals
    private val markValue = BoolValue("Mark", true)
    private val fakeSharpValue = BoolValue("FakeSharp", true)

    /**
     * MODULE
     */

    // Target
    var target: IEntityLivingBase? = null
    private var currentTarget: IEntityLivingBase? = null
    private var hitable = false
    private val prevTargetEntities = mutableListOf<Int>()

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0L
    private var clicks = 0

    // Container Delay
    private var containerOpen = -1L

    // Fake block status
    var blockingStatus = false

    init {
        cooldownValue.isSupported = Backend.REPRESENTED_BACKEND_VERSION != MinecraftVersion.MC_1_8
    }

    /**
     * Enable kill aura module
     */
    override fun onEnable() {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        updateTarget()
    }

    /**
     * Disable kill aura module
     */
    override fun onDisable() {
        target = null
        currentTarget = null
        hitable = false
        prevTargetEntities.clear()
        attackTimer.reset()
        clicks = 0

        stopBlocking()
    }

    /**
     * Motion event
     */
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.POST) {
            target ?: return
            currentTarget ?: return

            // Update hitable
            updateHitable()

            // AutoBlock
            if (autoBlockValue.get().equals("AfterTick", true) && canBlock)
                startBlocking(currentTarget!!, hitable)

            return
        }

        if (rotationStrafeValue.get().equals("Off", true))
            update()
    }

    /**
     * Strafe event
     */
    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (rotationStrafeValue.get().equals("Off", true))
            return

        update()

        if (currentTarget != null && RotationUtils.targetRotation != null) {
            when (rotationStrafeValue.get().toLowerCase()) {
                "strict" -> {
                    val (yaw) = RotationUtils.targetRotation ?: return
                    var strafe = event.strafe
                    var forward = event.forward
                    val friction = event.friction

                    var f = strafe * strafe + forward * forward

                    if (f >= 1.0E-4F) {
                        f = sqrt(f)

                        if (f < 1.0F)
                            f = 1.0F

                        f = friction / f
                        strafe *= f
                        forward *= f

                        val yawSin = sin((yaw * Math.PI / 180F).toFloat())
                        val yawCos = cos((yaw * Math.PI / 180F).toFloat())

                        val player = mc.thePlayer!!

                        player.motionX += strafe * yawCos - forward * yawSin
                        player.motionZ += forward * yawCos + strafe * yawSin
                    }
                    event.cancelEvent()
                }
                "silent" -> {
                    update()

                    RotationUtils.targetRotation.applyStrafeToPlayer(event)
                    event.cancelEvent()
                }
            }
        }
    }

    fun update() {
        if (cancelRun || (noInventoryAttackValue.get() && (classProvider.isGuiContainer(mc.currentScreen) ||
                        System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())))
            return

        // Update target
        updateTarget()

        if (target == null) {
            stopBlocking()
            return
        }

        // Target
        currentTarget = target

        if (!targetModeValue.get().equals("Switch", ignoreCase = true) && isEnemy(currentTarget))
            target = currentTarget
    }

    /**
     * Update event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (cancelRun) {
            target = null
            currentTarget = null
            hitable = false
            stopBlocking()
            return
        }

        if (noInventoryAttackValue.get() && (classProvider.isGuiContainer(mc.currentScreen) ||
                        System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())) {
            target = null
            currentTarget = null
            hitable = false
            if (classProvider.isGuiContainer(mc.currentScreen)) containerOpen = System.currentTimeMillis()
            return
        }

        if (target != null && currentTarget != null && (Backend.MINECRAFT_VERSION_MINOR == 8 || mc.thePlayer!!.getCooledAttackStrength(0.0F) >= cooldownValue.get())) {
            while (clicks > 0) {
                runAttack()
                clicks--
            }
        }
    }

    /**
     * Render event
     */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (cancelRun) {
            target = null
            currentTarget = null
            hitable = false
            stopBlocking()
            return
        }

        if (noInventoryAttackValue.get() && (classProvider.isGuiContainer(mc.currentScreen) ||
                        System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())) {
            target = null
            currentTarget = null
            hitable = false
            if (classProvider.isGuiContainer(mc.currentScreen)) containerOpen = System.currentTimeMillis()
            return
        }

        target ?: return

        if (markValue.get() && !targetModeValue.get().equals("Multi", ignoreCase = true))
            RenderUtils.drawPlatform(target, if (hitable) Color(37, 126, 255, 70) else Color(255, 0, 0, 70))

        if (currentTarget != null && attackTimer.hasTimePassed(attackDelay) &&
                currentTarget!!.hurtTime <= hurtTimeValue.get()) {
            clicks++
            attackTimer.reset()
            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())
        }
    }

    /**
     * Handle entity move
     */
    @EventTarget
    fun onEntityMove(event: EntityMovementEvent) {
        val movedEntity = event.movedEntity

        if (target == null || movedEntity != currentTarget)
            return

        updateHitable()
    }

    /**
     * Attack enemy
     */
    private fun runAttack() {
        target ?: return
        currentTarget ?: return
        val thePlayer = mc.thePlayer ?: return
        val theWorld = mc.theWorld ?: return

        // Settings
        val failRate = failRateValue.get()
        val swing = swingValue.get()
        val multi = targetModeValue.get().equals("Multi", ignoreCase = true)
        val openInventory = aacValue.get() && classProvider.isGuiContainer(mc.currentScreen)
        val failHit = failRate > 0 && Random().nextInt(100) <= failRate

        // Close inventory when open
        if (openInventory)
            mc.netHandler.addToSendQueue(classProvider.createCPacketCloseWindow())

        // Check is not hitable or check failrate

        if (!hitable || failHit) {
            if (swing && (fakeSwingValue.get() || failHit))
                thePlayer.swingItem()
        } else {
            // Attack
            if (!multi) {
                attackEntity(currentTarget!!)
            } else {
                var targets = 0

                for (entity in theWorld.loadedEntityList) {
                    val distance = thePlayer.getDistanceToEntityBox(entity)

                    if (classProvider.isEntityLivingBase(entity) && isEnemy(entity) && distance <= getRange(entity)) {
                        attackEntity(entity.asEntityLivingBase())

                        targets += 1

                        if (limitedMultiTargetsValue.get() != 0 && limitedMultiTargetsValue.get() <= targets)
                            break
                    }
                }
            }

            prevTargetEntities.add(if (aacValue.get()) target!!.entityId else currentTarget!!.entityId)

            if (target == currentTarget)
                target = null
        }

        // Open inventory
        if (openInventory)
            mc.netHandler.addToSendQueue(createOpenInventoryPacket())
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        // Reset fixed target to null
        target = null

        // Settings
        val hurtTime = hurtTimeValue.get()
        val fov = fovValue.get()
        val switchMode = targetModeValue.get().equals("Switch", ignoreCase = true)

        // Find possible targets
        val targets = mutableListOf<IEntityLivingBase>()

        val theWorld = mc.theWorld!!
        val thePlayer = mc.thePlayer!!

        for (entity in theWorld.loadedEntityList) {
            if (!classProvider.isEntityLivingBase(entity) || !isEnemy(entity) || (switchMode && prevTargetEntities.contains(entity.entityId)))
                continue

            val distance = thePlayer.getDistanceToEntityBox(entity)
            val entityFov = RotationUtils.getRotationDifference(entity)

            if (distance <= maxRange && (fov == 180F || entityFov <= fov) && entity.asEntityLivingBase().hurtTime <= hurtTime)
                targets.add(entity.asEntityLivingBase())
        }

        // Sort targets by priority
        when (priorityValue.get().toLowerCase()) {
            "distance" -> targets.sortBy { thePlayer.getDistanceToEntityBox(it) } // Sort by distance
            "health" -> targets.sortBy { it.health } // Sort by health
            "direction" -> targets.sortBy { RotationUtils.getRotationDifference(it) } // Sort by FOV
            "livingtime" -> targets.sortBy { -it.ticksExisted } // Sort by existence
        }

        // Find best target
        for (entity in targets) {
            // Update rotations to current target
            if (!updateRotations(entity)) // when failed then try another target
                continue

            // Set target to current entity
            target = entity
            return
        }

        // Cleanup last targets when no target found and try again
        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    /**
     * Check if [entity] is selected as enemy with current target options and other modules
     */
    private fun isEnemy(entity: IEntity?): Boolean {
        if (classProvider.isEntityLivingBase(entity) && entity != null && (EntityUtils.targetDead || isAlive(entity.asEntityLivingBase())) && entity != mc.thePlayer) {
            if (!EntityUtils.targetInvisible && entity.invisible)
                return false

            if (EntityUtils.targetPlayer && classProvider.isEntityPlayer(entity)) {
                val player = entity.asEntityPlayer()

                if (player.spectator || AntiBot.isBot(player))
                    return false

                if (player.isClientFriend() && !LiquidBounce.moduleManager[NoFriends::class.java].state)
                    return false

                val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams

                return !teams.state || !teams.isInYourTeam(entity.asEntityLivingBase())
            }

            return EntityUtils.targetMobs && entity.isMob() || EntityUtils.targetAnimals && entity.isAnimal()
        }

        return false
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: IEntityLivingBase) {
        // Stop blocking
        val thePlayer = mc.thePlayer!!

        if (thePlayer.isBlocking || blockingStatus)
            stopBlocking()

        // Call attack event
        LiquidBounce.eventManager.callEvent(AttackEvent(entity))

        // Attack target
        if (swingValue.get() && Backend.MINECRAFT_VERSION_MINOR == 8)
            thePlayer.swingItem()

        mc.netHandler.addToSendQueue(classProvider.createCPacketUseEntity(entity, ICPacketUseEntity.WAction.ATTACK))

        if (swingValue.get() && Backend.MINECRAFT_VERSION_MINOR != 8)
            thePlayer.swingItem()

        if (keepSprintValue.get()) {
            // Critical Effect
            if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder &&
                    !thePlayer.isInWater && !thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.BLINDNESS)) && !thePlayer.isRiding)
                thePlayer.onCriticalHit(entity)

            // Enchant Effect
            if (functions.getModifierForCreature(thePlayer.heldItem, entity.creatureAttribute) > 0F)
                thePlayer.onEnchantmentCritical(entity)
        } else {
            if (mc.playerController.currentGameType != IWorldSettings.WGameType.SPECTATOR)
                thePlayer.attackTargetEntityWithCurrentItem(entity)
        }

        // Extra critical effects
        val criticals = LiquidBounce.moduleManager[Criticals::class.java] as Criticals

        for (i in 0..2) {
            // Critical Effect
            if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.BLINDNESS)) && thePlayer.ridingEntity == null || criticals.state && criticals.msTimer.hasTimePassed(criticals.delayValue.get().toLong()) && !thePlayer.isInWater && !thePlayer.isInLava && !thePlayer.isInWeb)
                thePlayer.onCriticalHit(target!!)

            // Enchant Effect
            if (functions.getModifierForCreature(thePlayer.heldItem, target!!.creatureAttribute) > 0.0f || fakeSharpValue.get())
                thePlayer.onEnchantmentCritical(target!!)
        }

        // Start blocking after attack
        if (autoBlockValue.get().equals("Packet", true) && (thePlayer.isBlocking || canBlock))
            startBlocking(entity, interactAutoBlockValue.get())

        @Suppress("ConstantConditionIf")
        if (Backend.MINECRAFT_VERSION_MINOR != 8) {
            thePlayer.resetCooldown()
        }
    }

    /**
     * Update killaura rotations to enemy
     */
    private fun updateRotations(entity: IEntity): Boolean {
        if (maxTurnSpeed.get() <= 0F)
            return true

        var boundingBox = entity.entityBoundingBox

        if (predictValue.get())
            boundingBox = boundingBox.offset(
                    (entity.posX - entity.prevPosX - (mc.thePlayer!!.posX - mc.thePlayer!!.prevPosX)) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                    (entity.posY - entity.prevPosY - (mc.thePlayer!!.posY - mc.thePlayer!!.prevPosY)) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                    (entity.posZ - entity.prevPosZ - (mc.thePlayer!!.posZ - mc.thePlayer!!.prevPosZ)) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get())
            )

        val (_, rotation) = RotationUtils.searchCenter(
                boundingBox,
                outborderValue.get() && !attackTimer.hasTimePassed(attackDelay / 2),
                randomCenterValue.get(),
                predictValue.get(),
                mc.thePlayer!!.getDistanceToEntityBox(entity) < throughWallsRangeValue.get(),
                maxRange
        ) ?: return false

        val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation,
                (Math.random() * (maxTurnSpeed.get() - minTurnSpeed.get()) + minTurnSpeed.get()).toFloat())

        if (silentRotationValue.get())
            RotationUtils.setTargetRotation(limitedRotation, if (aacValue.get()) 15 else 0)
        else
            limitedRotation.toPlayer(mc.thePlayer!!)

        return true
    }

    /**
     * Check if enemy is hitable with current rotations
     */
    private fun updateHitable() {
        // Disable hitable check if turn speed is zero
        if (maxTurnSpeed.get() <= 0F) {
            hitable = true
            return
        }

        val reach = min(maxRange.toDouble(), mc.thePlayer!!.getDistanceToEntityBox(target!!)) + 1

        if (raycastValue.get()) {
            val raycastedEntity = RaycastUtils.raycastEntity(reach, object : RaycastUtils.EntityFilter {
                override fun canRaycast(entity: IEntity?): Boolean {
                    return (!livingRaycastValue.get() || (classProvider.isEntityLivingBase(entity) && !classProvider.isEntityArmorStand(entity))) &&
                            (isEnemy(entity) || raycastIgnoredValue.get() || aacValue.get() && mc.theWorld!!.getEntitiesWithinAABBExcludingEntity(entity, entity!!.entityBoundingBox).isNotEmpty())
                }

            })

            if (raycastValue.get() && raycastedEntity != null && classProvider.isEntityLivingBase(raycastedEntity)
                    && (LiquidBounce.moduleManager[NoFriends::class.java].state || !(classProvider.isEntityPlayer(raycastedEntity) && raycastedEntity.asEntityPlayer().isClientFriend())))
                currentTarget = raycastedEntity.asEntityLivingBase()

            hitable = if (maxTurnSpeed.get() > 0F) currentTarget == raycastedEntity else true
        } else
            hitable = RotationUtils.isFaced(currentTarget, reach)
    }

    /**
     * Start blocking
     */
    private fun startBlocking(interactEntity: IEntity, interact: Boolean) {
        if (!(blockRate.get() > 0 && Random().nextInt(100) <= blockRate.get()))
            return

        if (interact) {
            val positionEye = mc.renderViewEntity?.getPositionEyes(1F)

            val expandSize = interactEntity.collisionBorderSize.toDouble()
            val boundingBox = interactEntity.entityBoundingBox.expand(expandSize, expandSize, expandSize)

            val (yaw, pitch) = RotationUtils.targetRotation ?: Rotation(mc.thePlayer!!.rotationYaw, mc.thePlayer!!.rotationPitch)
            val yawCos = cos(-yaw * 0.017453292F - Math.PI.toFloat())
            val yawSin = sin(-yaw * 0.017453292F - Math.PI.toFloat())
            val pitchCos = -cos(-pitch * 0.017453292F)
            val pitchSin = sin(-pitch * 0.017453292F)
            val range = min(maxRange.toDouble(), mc.thePlayer!!.getDistanceToEntityBox(interactEntity)) + 1
            val lookAt = positionEye!!.addVector(yawSin * pitchCos * range, pitchSin * range, yawCos * pitchCos * range)

            val movingObject = boundingBox.calculateIntercept(positionEye, lookAt) ?: return
            val hitVec = movingObject.hitVec

            mc.netHandler.addToSendQueue(classProvider.createCPacketUseEntity(interactEntity, WVec3(
                    hitVec.xCoord - interactEntity.posX,
                    hitVec.yCoord - interactEntity.posY,
                    hitVec.zCoord - interactEntity.posZ)
            ))
            mc.netHandler.addToSendQueue(classProvider.createCPacketUseEntity(interactEntity, ICPacketUseEntity.WAction.INTERACT))
        }

        mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerBlockPlacement(WBlockPos(-1, -1, -1),
                255, mc.thePlayer!!.inventory.getCurrentItemInHand(), 0.0F, 0.0F, 0.0F))
        blockingStatus = true
    }


    /**
     * Stop blocking
     */
    private fun stopBlocking() {
        if (blockingStatus) {
            mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, classProvider.getEnumFacing(EnumFacingType.DOWN)))
            blockingStatus = false
        }
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun: Boolean
        inline get() = mc.thePlayer!!.spectator || !isAlive(mc.thePlayer!!)
                || LiquidBounce.moduleManager[Blink::class.java].state || LiquidBounce.moduleManager[FreeCam::class.java].state

    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: IEntityLivingBase) = entity.entityAlive && entity.health > 0 ||
            aacValue.get() && entity.hurtTime > 5

    /**
     * Check if player is able to block
     */
    private val canBlock: Boolean
        inline get() = mc.thePlayer!!.heldItem != null && classProvider.isItemSword(mc.thePlayer!!.heldItem!!.item) && Backend.MINECRAFT_VERSION_MINOR == 8

    /**
     * Range
     */
    private val maxRange: Float
        get() = max(rangeValue.get(), throughWallsRangeValue.get())

    private fun getRange(entity: IEntity) =
            (if (mc.thePlayer!!.getDistanceToEntityBox(entity) >= throughWallsRangeValue.get()) rangeValue.get() else throughWallsRangeValue.get()) - if (mc.thePlayer!!.sprinting) rangeSprintReducementValue.get() else 0F

    /**
     * HUD Tag
     */
    override val tag: String?
        get() = targetModeValue.get()

    val isBlockingChestAura: Boolean
        get() = state && target != null
}