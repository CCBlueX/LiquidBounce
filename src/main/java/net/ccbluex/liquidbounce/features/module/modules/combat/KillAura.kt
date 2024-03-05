/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.CooldownHelper.getAttackCooldownProgress
import net.ccbluex.liquidbounce.utils.CooldownHelper.resetLastAttackedTicks
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.EntityUtils.targetAnimals
import net.ccbluex.liquidbounce.utils.EntityUtils.targetDead
import net.ccbluex.liquidbounce.utils.EntityUtils.targetInvisible
import net.ccbluex.liquidbounce.utils.EntityUtils.targetMobs
import net.ccbluex.liquidbounce.utils.EntityUtils.targetPlayer
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.isRotationFaced
import net.ccbluex.liquidbounce.utils.RotationUtils.isVisible
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.ItemUtils.isConsumingItem
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.network.play.client.C02PacketUseEntity.Action.INTERACT
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.max

object KillAura : Module("KillAura", ModuleCategory.COMBAT, Keyboard.KEY_R) {
    /**
     * OPTIONS
     */

    private val simulateCooldown by BoolValue("SimulateCooldown", false)
    private val simulateDoubleClicking by BoolValue("SimulateDoubleClicking", false) { !simulateCooldown }

    // CPS - Attack speed
    private val maxCPSValue = object : IntegerValue("MaxCPS", 8, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minCPS)

        override fun onChanged(oldValue: Int, newValue: Int) {
            attackDelay = randomClickDelay(minCPS, newValue)
        }

        override fun isSupported() = !simulateCooldown
    }

    private val maxCPS by maxCPSValue

    private val minCPS: Int by object : IntegerValue("MinCPS", 5, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxCPS)

        override fun onChanged(oldValue: Int, newValue: Int) {
            attackDelay = randomClickDelay(newValue, maxCPS)
        }

        override fun isSupported() = !maxCPSValue.isMinimal() && !simulateCooldown
    }

    private val hurtTime by IntegerValue("HurtTime", 10, 0..10) { !simulateCooldown }

    private val clickOnly by BoolValue("ClickOnly", false)

    // Range
    // TODO: Make block range independent from attack range
    private val range: Float by object : FloatValue("Range", 3.7f, 1f..8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            blockRange = blockRange.coerceAtMost(newValue)
        }
    }
    private val scanRange by FloatValue("ScanRange", 2f, 0f..10f)
    private val throughWallsRange by FloatValue("ThroughWallsRange", 3f, 0f..8f)
    private val rangeSprintReduction by FloatValue("RangeSprintReduction", 0f, 0f..0.4f)

    // Modes
    private val priority by ListValue(
        "Priority", arrayOf(
            "Health",
            "Distance",
            "Direction",
            "LivingTime",
            "Armor",
            "HurtResistance",
            "HurtTime",
            "HealthAbsorption",
            "RegenAmplifier"
        ), "Distance"
    )
    private val targetMode by ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val limitedMultiTargets by IntegerValue("LimitedMultiTargets", 0, 0..50) { targetMode == "Multi" }
    private val maxSwitchFOV by FloatValue("MaxSwitchFOV", 90f, 30f..180f) { targetMode == "Switch" }

    // Delay
    private val switchDelay by IntegerValue("SwitchDelay", 15, 1..1000) { targetMode == "Switch" }

    // Bypass
    private val swing by BoolValue("Swing", true)
    private val keepSprint by BoolValue("KeepSprint", true)

    // Settings
    private val onScaffold by BoolValue("OnScaffold", false)

    // AutoBlock
    private val autoBlock by ListValue("AutoBlock", arrayOf("Off", "Packet", "Fake"), "Packet")
    private val releaseAutoBlock by BoolValue("ReleaseAutoBlock", true)
    { autoBlock !in arrayOf("Off", "Fake") }
    private val ignoreTickRule by BoolValue("IgnoreTickRule", false)
    { autoBlock !in arrayOf("Off", "Fake") && releaseAutoBlock }
    private val blockRate by IntegerValue("BlockRate", 100, 1..100)
    { autoBlock !in arrayOf("Off", "Fake") && releaseAutoBlock }

    private val uncpAutoBlock by BoolValue("UpdatedNCPAutoBlock", false)
    { autoBlock !in arrayOf("Off", "Fake") && !releaseAutoBlock }

    private val switchStartBlock by BoolValue("SwitchStartBlock", false)
    { autoBlock !in arrayOf("Off", "Fake") }

    private val interactAutoBlock by BoolValue("InteractAutoBlock", true)
    { autoBlock !in arrayOf("Off", "Fake") }

    // AutoBlock conditions
    private val smartAutoBlock by BoolValue("SmartAutoBlock", false) { autoBlock != "Off" }

    // Ignore all blocking conditions, except for block rate, when standing still
    private val forceBlock by BoolValue("ForceBlockWhenStill", true)
    { autoBlock != "Off" && smartAutoBlock }

    // Don't block if target isn't holding a sword or an axe
    private val checkWeapon by BoolValue("CheckEnemyWeapon", true)
    { autoBlock != "Off" && smartAutoBlock }

    // TODO: Make block range independent from attack range
    private var blockRange by object : FloatValue("BlockRange", range, 1f..8f) {
        override fun isSupported() = autoBlock != "Off" && smartAutoBlock

        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(this@KillAura.range)
    }

    // Don't block when you can't get damaged
    private val maxOwnHurtTime by IntegerValue("MaxOwnHurtTime", 3, 0..10)
    { autoBlock != "Off" && smartAutoBlock }

    // Don't block if target isn't looking at you
    private val maxDirectionDiff by FloatValue("MaxOpponentDirectionDiff", 60f, 30f..180f)
    { autoBlock != "Off" && smartAutoBlock }

    // Don't block if target is swinging an item and therefore cannot attack
    private val maxSwingProgress by IntegerValue("MaxOpponentSwingProgress", 1, 0..5)
    { autoBlock != "Off" && smartAutoBlock }

    // Turn Speed
    private val maxTurnSpeedValue = object : FloatValue("MaxTurnSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minTurnSpeed)
    }
    private val maxTurnSpeed by maxTurnSpeedValue

    private val minTurnSpeed: Float by object : FloatValue("MinTurnSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxTurnSpeed)
        override fun isSupported() = !maxTurnSpeedValue.isMinimal()
    }

    // Raycast
    private val raycastValue = BoolValue("RayCast", true)
    private val raycast by raycastValue
    private val raycastIgnored by BoolValue("RayCastIgnored", false) { raycastValue.isActive() }
    private val livingRaycast by BoolValue("LivingRayCast", true) { raycastValue.isActive() }

    // Hit delay
    private val useHitDelay by BoolValue("UseHitDelay", false)
    private val hitDelayTicks by IntegerValue("HitDelayTicks", 1, 1..5) { useHitDelay }

    // Rotations
    private val keepRotationTicks by object : IntegerValue("KeepRotationTicks", 5, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
    }
    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f)
    private val micronizedValue = BoolValue("Micronized", true)
    private val micronized by micronizedValue
    private val micronizedStrength by FloatValue("MicronizedStrength", 0.8f, 0.2f..2f) { micronizedValue.isActive() }
    private val silentRotationValue = BoolValue("SilentRotation", true)
    private val silentRotation by silentRotationValue
    private val rotationStrafe by ListValue("Strafe",
        arrayOf("Off", "Strict", "Silent"),
        "Off"
    ) { silentRotationValue.isActive() }
    private val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative")

    private val randomCenter by BoolValue("RandomCenter", true)
    private val gaussianOffset by BoolValue("GaussianOffset", false) { randomCenter }
    private val outborder by BoolValue("Outborder", false)
    private val fov by FloatValue("FOV", 180f, 0f..180f)

    // Prediction
    private val predictClientMovement by IntegerValue("PredictClientMovement", 2, 0..5)
    private val predictEnemyPosition by FloatValue("PredictEnemyPosition", 1.5f, -1f..2f)

    // Extra swing
    private val failSwing by BoolValue("FailSwing", true) { swing }
    private val swingOnlyInAir by BoolValue("SwingOnlyInAir", true) { swing && failSwing }
    private val maxRotationDifferenceToSwing by FloatValue("MaxRotationDifferenceToSwing", 180f, 0f..180f)
    { swing && failSwing }
    private val swingWhenTicksLate = object : BoolValue("SwingWhenTicksLate", false) {
        override fun isSupported() = swing && failSwing && maxRotationDifferenceToSwing != 180f
    }
    private val ticksLateToSwing by IntegerValue("TicksLateToSwing", 4, 0..20)
    { swing && failSwing && swingWhenTicksLate.isActive() }

    // Inventory
    private val simulateClosingInventory by BoolValue("SimulateClosingInventory", false) { !noInventoryAttack }
    private val noInventoryAttack by BoolValue("NoInvAttack", false, subjective = true)
    private val noInventoryDelay by IntegerValue("NoInvDelay", 200, 0..500, subjective = true) { noInventoryAttack }
    private val noConsumeAttack by ListValue("NoConsumeAttack",
        arrayOf("Off", "NoHits", "NoRotation"),
        "Off",
        subjective = true
    )

    // Visuals
    private val mark by BoolValue("Mark", true, subjective = true)
    private val fakeSharp by BoolValue("FakeSharp", true, subjective = true)

    /**
     * MODULE
     */

    // Target
    var target: EntityLivingBase? = null
    private var hittable = false
    private val prevTargetEntities = mutableListOf<Int>()

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0
    private var clicks = 0
    private var attackTickTimes = mutableListOf<Pair<MovingObjectPosition, Int>>()

    // Container Delay
    private var containerOpen = -1L

    // Block status
    var renderBlocking = false
    var blockStatus = false
    private var blockStopInDead = false

    /**
     * Disable kill aura module
     */
    override fun onToggle(state: Boolean) {
        target = null
        hittable = false
        prevTargetEntities.clear()
        attackTickTimes.clear()
        attackTimer.reset()
        clicks = 0

        stopBlocking()
    }

    /**
     * Motion event
     */
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST) {
            return
        }

        update()
    }

    fun update() {
        if (cancelRun || (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay))) return

        // Update target
        updateTarget()
    }

    @EventTarget
    fun onWorldChange(event: WorldEvent) {
        attackTickTimes.clear()
    }

    /**
     * Tick event
     */
    @EventTarget
    fun onTick(event: TickEvent) {
        if (clickOnly && !mc.gameSettings.keyBindAttack.isKeyDown) return

        if (blockStatus && autoBlock == "Packet" && releaseAutoBlock && !ignoreTickRule) {
            clicks = 0
            stopBlocking()
            return
        }

        if (cancelRun) {
            target = null
            hittable = false
            stopBlocking()
            return
        }

        if (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return
        }

        if (simulateCooldown && getAttackCooldownProgress() < 1f) {
            return
        }

        if (target == null && !blockStopInDead) {
            blockStopInDead = true
            stopBlocking()
            return
        }

        if (target != null) {
            if (mc.thePlayer.getDistanceToEntityBox(target!!) > range && blockStatus) {
                stopBlocking()
                return
            }

            // Usually when you butterfly click, you end up clicking two (and possibly more) times in a single tick.
            // Sometimes you also do not click. The positives outweigh the negatives, however.
            val extraClicks = if (simulateDoubleClicking && !simulateCooldown) nextInt(-1, 1) else 0

            val maxClicks = clicks + extraClicks

            repeat(maxClicks) {
                val wasBlocking = blockStatus

                runAttack(it + 1 == maxClicks)
                clicks--

                if (wasBlocking && !blockStatus && (releaseAutoBlock && !ignoreTickRule || autoBlock == "Off")) {
                    return
                }
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
            hittable = false
            return
        }

        if (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return
        }

        target ?: return

        if (mark && targetMode != "Multi") {
            drawPlatform(target!!, if (hittable) Color(37, 126, 255, 70) else Color(255, 0, 0, 70))
        }

        if (attackTimer.hasTimePassed(attackDelay)) {
            if (maxCPS > 0)
                clicks++
            attackTimer.reset()
            attackDelay = randomClickDelay(minCPS, maxCPS)
        }
    }

    /**
     * Attack enemy
     */
    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    private fun runAttack(isLastClick: Boolean) {
        var currentTarget = this.target ?: return

        val thePlayer = mc.thePlayer ?: return
        val theWorld = mc.theWorld ?: return

        if (noConsumeAttack == "NoHits" && isConsumingItem()) {
            return
        }

        // Settings
        val multi = targetMode == "Multi"
        val manipulateInventory = simulateClosingInventory && !noInventoryAttack && serverOpenInventory

        // Close inventory when open
        if (manipulateInventory) serverOpenInventory = false

        updateHittable()

        currentTarget = this.target ?: return

        if (hittable && currentTarget.hurtTime > hurtTime) {
            return
        }

        // Check if enemy is not hittable
        if (!hittable) {
            if (swing && failSwing) {
                val rotation = currentRotation ?: thePlayer.rotation

                // Can humans keep click consistency when performing massive rotation changes?
                // (10-30 rotation difference/doing large mouse movements for example)
                // Maybe apply to attacks too?
                if (getRotationDifference(rotation) > maxRotationDifferenceToSwing) {
                    val lastAttack = attackTickTimes.lastOrNull()?.second ?: 0

                    // At the same time there is also a chance of the user clicking at least once in a while
                    // when the consistency has dropped a lot.
                    val shouldIgnore = swingWhenTicksLate.isActive() && runTimeTicks - lastAttack >= ticksLateToSwing

                    if (!shouldIgnore) {
                        return
                    }
                }

                runWithModifiedRaycastResult(rotation, range.toDouble(), throughWallsRange.toDouble()) {
                    if (swingOnlyInAir && it.typeOfHit != MovingObjectPosition.MovingObjectType.MISS) {
                        return@runWithModifiedRaycastResult
                    }

                    if (!shouldDelayClick(it.typeOfHit)) {
                        if (it.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                            val entity = it.entityHit

                            // Use own function instead of clickMouse() to maintain keep sprint, auto block, etc
                            if (entity is EntityLivingBase) {
                                attackEntity(entity)
                            }
                        } else {
                            // Imitate game click
                            mc.clickMouse()
                        }
                        attackTickTimes += it to runTimeTicks
                    }

                    if (isLastClick) {
                        // We return false because when you click literally once, the attack key's [pressed] status is false.
                        // Since we simulate clicks, we are supposed to respect that behavior.
                        mc.sendClickBlockToController(false)
                    }
                }
            }
        } else {
            blockStopInDead = false
            // Attack
            if (!multi) {
                attackEntity(currentTarget)
            } else {
                var targets = 0

                for (entity in theWorld.loadedEntityList) {
                    val distance = thePlayer.getDistanceToEntityBox(entity)

                    if (entity is EntityLivingBase && isEnemy(entity) && distance <= getRange(entity)) {
                        attackEntity(entity)

                        targets += 1

                        if (limitedMultiTargets != 0 && limitedMultiTargets <= targets) break
                    }
                }
            }

            prevTargetEntities += currentTarget.entityId
        }

        if (targetMode.equals("Switch", ignoreCase = true) && attackTimer.hasTimePassed((switchDelay).toLong())) {
            if (switchDelay != 0) {
                prevTargetEntities += currentTarget.entityId
                attackTimer.reset()
            }
        }

        // Open inventory
        if (manipulateInventory) serverOpenInventory = true
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        if (!onScaffold && Scaffold.state)
            return

        // Reset fixed target to null
        target = null

        // Settings
        val fov = fov
        val switchMode = targetMode == "Switch"

        // Find possible targets
        val targets = mutableListOf<EntityLivingBase>()

        val theWorld = mc.theWorld
        val thePlayer = mc.thePlayer

        for (entity in theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !isEnemy(entity) || (switchMode && entity.entityId in prevTargetEntities)) continue

            var distance = thePlayer.getDistanceToEntityBox(entity)

            if (Backtrack.handleEvents()) {
                val trackedDistance = Backtrack.getNearestTrackedDistance(entity)

                if (distance > trackedDistance) {
                    distance = trackedDistance
                }
            }

            val entityFov = getRotationDifference(entity)

            if (distance <= maxRange && (fov == 180F || entityFov <= fov)) {
                if (isLookingOnEntities(entity, maxSwitchFOV.toDouble())) {
                    targets += entity
                }
            }
        }

        // Sort targets by priority
        when (priority.lowercase()) {
            "distance" -> {
                targets.sortBy {
                    var result = 0.0

                    Backtrack.runWithNearestTrackedDistance(it) {
                        result = thePlayer.getDistanceToEntityBox(it) // Sort by distance
                    }

                    result
                }
            }

            "direction" -> targets.sortBy {
                var result = 0f

                Backtrack.runWithNearestTrackedDistance(it) {
                    result = getRotationDifference(it) // Sort by FOV
                }

                result
            }

            "health" -> targets.sortBy { it.health } // Sort by health
            "livingtime" -> targets.sortBy { -it.ticksExisted } // Sort by existence
            "armor" -> targets.sortBy { it.totalArmorValue } // Sort by armor
            "hurtresistance" -> targets.sortBy { it.hurtResistantTime } // Sort by armor hurt time
            "hurttime" -> targets.sortBy { it.hurtTime } // Sort by hurt time
            "healthabsorption" -> targets.sortBy { it.health + it.absorptionAmount } // Sort by full health with absorption effect
            "regenamplifier" -> targets.sortBy {
                if (it.isPotionActive(Potion.regeneration)) it.getActivePotionEffect(
                    Potion.regeneration
                ).amplifier else -1
            }
        }

        // Find best target
        for (entity in targets) {
            // Update rotations to current target
            var success = false

            Backtrack.runWithNearestTrackedDistance(entity) {
                success = updateRotations(entity)
            }

            if (!success) {
                // when failed then try another target
                continue
            }

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
    private fun isEnemy(entity: Entity?): Boolean {
        if (entity is EntityLivingBase && (targetDead || isAlive(entity)) && entity != mc.thePlayer) {
            if (!targetInvisible && entity.isInvisible) return false

            if (targetPlayer && entity is EntityPlayer) {
                if (entity.isSpectator || isBot(entity)) return false

                if (entity.isClientFriend() && !NoFriends.handleEvents()) return false

                return !Teams.handleEvents() || !Teams.isInYourTeam(entity)
            }

            return targetMobs && entity.isMob() || targetAnimals && entity.isAnimal()
        }

        return false
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: EntityLivingBase) {
        // Stop blocking
        val thePlayer = mc.thePlayer

        if (!onScaffold && Scaffold.state)
            return

        if ((thePlayer.isBlocking || renderBlocking) && (autoBlock == "Off" && blockStatus || autoBlock == "Packet" && releaseAutoBlock)) {
            stopBlocking()

            if (!ignoreTickRule || autoBlock == "Off") {
                return
            }
        }

        // The function is only called when we are facing an entity
        if (shouldDelayClick(MovingObjectPosition.MovingObjectType.ENTITY)) {
            return
        }

        // Call attack event
        callEvent(AttackEvent(entity))

        // Attack target
        if (swing) thePlayer.swingItem()

        sendPacket(C02PacketUseEntity(entity, ATTACK))

        if (keepSprint && !KeepSprint.state) {
            // Critical Effect
            if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(
                    Potion.blindness
                ) && !thePlayer.isRiding) {
                thePlayer.onCriticalHit(entity)
            }

            // Enchant Effect
            if (EnchantmentHelper.getModifierForCreature(thePlayer.heldItem, entity.creatureAttribute) > 0F) {
                thePlayer.onEnchantmentCritical(entity)
            }
        } else {
            if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR) {
                thePlayer.attackTargetEntityWithCurrentItem(entity)
            }
        }

        // Extra critical effects
        repeat(3) {
            // Critical Effect
            if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(
                    Potion.blindness
                ) && thePlayer.ridingEntity == null || Criticals.handleEvents() && Criticals.msTimer.hasTimePassed(
                    Criticals.delay
                ) && !thePlayer.isInWater && !thePlayer.isInLava && !thePlayer.isInWeb) {
                thePlayer.onCriticalHit(entity)
            }

            // Enchant Effect
            if (EnchantmentHelper.getModifierForCreature(thePlayer.heldItem,
                    entity.creatureAttribute
                ) > 0f || fakeSharp) {
                thePlayer.onEnchantmentCritical(entity)
            }
        }

        CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)

        // Start blocking after attack
        if (autoBlock != "Off" && (thePlayer.isBlocking || canBlock)) {
            startBlocking(entity, interactAutoBlock, autoBlock == "Fake")
        }

        resetLastAttackedTicks()
    }

    /**
     * Update killaura rotations to enemy
     */
    private fun updateRotations(entity: Entity): Boolean {
        val player = mc.thePlayer ?: return false

        if (!onScaffold && Scaffold.state)
            return false

        val (predictX, predictY, predictZ) = entity.currPos.subtract(entity.prevPos)
            .times(2 + predictEnemyPosition.toDouble())

        val boundingBox = entity.hitBox.offset(predictX, predictY, predictZ)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(player.movementInput)

        repeat(predictClientMovement + 1) {
            simPlayer.tick()
        }

        player.setPosAndPrevPos(simPlayer.pos)

        val rotation = searchCenter(
            boundingBox,
            outborder && !attackTimer.hasTimePassed(attackDelay / 2),
            randomCenter,
            gaussianOffset = this.gaussianOffset,
            predict = false,
            lookRange = range + scanRange,
            attackRange = range,
            throughWallsRange = throughWallsRange
        )

        if (rotation == null) {
            player.setPosAndPrevPos(currPos, oldPos)

            return false
        }

        // Get our current rotation. Otherwise, player rotation.
        val currentRotation = currentRotation ?: player.rotation

        var limitedRotation = limitAngleChange(currentRotation,
            rotation,
            nextFloat(minTurnSpeed, maxTurnSpeed),
            smootherMode
        )

        if (micronized) {
            // Is player facing the entity with current rotation?
            if (isRotationFaced(entity, maxRange.toDouble(), currentRotation)) {
                // Limit angle change but this time modify the turn speed.
                limitedRotation = limitAngleChange(currentRotation, rotation,
                    nextFloat(endInclusive = micronizedStrength)
                )
            }
        }

        if (silentRotation) {
            setTargetRotation(
                limitedRotation,
                keepRotationTicks,
                !(!silentRotation || rotationStrafe == "Off"),
                rotationStrafe == "Strict",
                minTurnSpeed to maxTurnSpeed,
                angleThresholdUntilReset,
                smootherMode
            )
        } else {
            limitedRotation.toPlayer(player)
        }

        player.setPosAndPrevPos(currPos, oldPos)

        return true
    }

    /**
     * Check if enemy is hittable with current rotations
     */
    private fun updateHittable() {
        val eyes = mc.thePlayer.eyes

        val currentRotation = currentRotation ?: mc.thePlayer.rotation
        val target = this.target ?: return

        if (!onScaffold && Scaffold.state)
            return

        var chosenEntity: Entity? = null

        if (raycast) {
            chosenEntity = raycastEntity(range.toDouble(),
                currentRotation.yaw,
                currentRotation.pitch
            ) { entity -> !livingRaycast || entity is EntityLivingBase && entity !is EntityArmorStand }

            if (chosenEntity != null && chosenEntity is EntityLivingBase && (NoFriends.handleEvents() || !(chosenEntity is EntityPlayer && chosenEntity.isClientFriend()))) {
                if (raycastIgnored && target != chosenEntity) {
                    this.target = chosenEntity
                }
            }

            hittable = this.target == chosenEntity
        } else {
            hittable = isRotationFaced(target, range.toDouble(), currentRotation)
        }

        if (!hittable) {
            return
        }

        val targetToCheck = chosenEntity ?: this.target ?: return

        // If player is inside entity, automatic yes because the intercept below cannot check for that
        // Minecraft does the same, see #EntityRenderer line 353
        if (targetToCheck.hitBox.isVecInside(eyes)) {
            return
        }

        var checkNormally = true

        if (Backtrack.handleEvents()) {
            Backtrack.loopThroughBacktrackData(targetToCheck) {
                if (targetToCheck.hitBox.isVecInside(eyes)) {
                    checkNormally = false
                    return@loopThroughBacktrackData true
                }

                // Recreate raycast logic
                val intercept = targetToCheck.hitBox.calculateIntercept(eyes,
                    eyes + getVectorForRotation(currentRotation) * range.toDouble()
                )

                if (intercept != null) {
                    // Is the entity box raycast vector visible? If not, check through-wall range
                    hittable = isVisible(intercept.hitVec) || mc.thePlayer.getDistanceToEntityBox(targetToCheck) <= throughWallsRange

                    if (hittable) {
                        checkNormally = false
                        return@loopThroughBacktrackData true
                    }
                }

                return@loopThroughBacktrackData false
            }
        }

        if (!checkNormally) {
            return
        }

        // Recreate raycast logic
        val intercept = targetToCheck.hitBox.calculateIntercept(eyes,
            eyes + getVectorForRotation(currentRotation) * range.toDouble()
        )

        // Is the entity box raycast vector visible? If not, check through-wall range
        hittable = isVisible(intercept.hitVec) || mc.thePlayer.getDistanceToEntityBox(targetToCheck) <= throughWallsRange
    }

    /**
     * Start blocking
     */
    private fun startBlocking(interactEntity: Entity, interact: Boolean, fake: Boolean = false) {
        if (blockStatus && !uncpAutoBlock)
            return

        if (!onScaffold && Scaffold.state)
            return

        if (mc.thePlayer.isBlocking) {
            blockStatus = true
            renderBlocking = true
            return
        }

        if (!fake) {
            if (!(blockRate > 0 && nextInt(endExclusive = 100) <= blockRate)) return

            if (interact) {
                val positionEye = mc.thePlayer.eyes

                val boundingBox = interactEntity.hitBox

                val (yaw, pitch) = currentRotation ?: mc.thePlayer.rotation

                val vec = getVectorForRotation(Rotation(yaw, pitch))

                val lookAt = positionEye.add(vec * maxRange.toDouble())

                val movingObject = boundingBox.calculateIntercept(positionEye, lookAt) ?: return
                val hitVec = movingObject.hitVec

                sendPackets(
                    C02PacketUseEntity(interactEntity, hitVec - interactEntity.positionVector),
                    C02PacketUseEntity(interactEntity, INTERACT)
                )

            }

            if (switchStartBlock) {
                InventoryUtils.serverSlot = (InventoryUtils.serverSlot + 1) % 9
                InventoryUtils.serverSlot = mc.thePlayer.inventory.currentItem
            }

            sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
            blockStatus = true
        }

        renderBlocking = true

        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
    }

    /**
     * Stop blocking
     */
    private fun stopBlocking() {
        if (blockStatus && !mc.thePlayer.isBlocking) {
            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            blockStatus = false
        }

        renderBlocking = false
    }

    /**
     * Check if raycast landed on a different object
     *
     * The game requires at least 1 tick of cooldown on raycast object type change (miss, block, entity)
     * We are doing the same thing here but allow more cool down.
     */
    private fun shouldDelayClick(type: MovingObjectPosition.MovingObjectType): Boolean {
        if (!useHitDelay) {
            return false
        }

        val lastAttack = attackTickTimes.lastOrNull()

        return lastAttack != null && lastAttack.first.typeOfHit != type && runTimeTicks - lastAttack.second <= hitDelayTicks
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun
        inline get() = mc.thePlayer.isSpectator || !isAlive(mc.thePlayer) || FreeCam.handleEvents() || (noConsumeAttack == "NoRotation" && isConsumingItem())

    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0

    /**
     * Check if player is able to block
     */
    private val canBlock: Boolean
        get() {
            if (target != null && mc.thePlayer?.heldItem?.item is ItemSword) {
                if (smartAutoBlock) {
                    if (!isMoving && forceBlock) return true

                    if (checkWeapon && (target!!.heldItem?.item !is ItemSword && target!!.heldItem?.item !is ItemAxe))
                        return false

                    if (mc.thePlayer.hurtTime > maxOwnHurtTime) return false

                    val rotationToPlayer = toRotation(mc.thePlayer.hitBox.center, true, target!!)

                    if (getRotationDifference(rotationToPlayer, target!!.rotation) > maxDirectionDiff)
                        return false

                    if (target!!.swingProgressInt > maxSwingProgress) return false

                    if (target!!.getDistanceToEntityBox(mc.thePlayer) > blockRange) return false
                }

                return true
            }

            return false
        }

    /**
     * Range
     */
    private val maxRange
        get() = max(range + scanRange, throughWallsRange)

    private fun getRange(entity: Entity) =
        (if (mc.thePlayer.getDistanceToEntityBox(entity) >= throughWallsRange) range + scanRange else throughWallsRange) - if (mc.thePlayer.isSprinting) rangeSprintReduction else 0F

    /**
     * HUD Tag
     */
    override val tag
        get() = targetMode

    val isBlockingChestAura
        get() = handleEvents() && target != null
}
