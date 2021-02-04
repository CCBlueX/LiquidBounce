/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import java.awt.Color
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

@ModuleInfo(name = "AntiBot", description = "Prevents KillAura from attacking AntiCheat bots.", category = ModuleCategory.MISC)
object AntiBot : Module()
{

	// Tab
	private val tabValue = BoolValue("Tab", true)
	private val tabModeValue = ListValue("TabMode", arrayOf("Equals", "Contains"), "Contains")
	private val tabStripColorsValue = BoolValue("TabStripColorsInDisplayname", true)
	private val tabNameModeValue = ListValue("TabNameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName")

	// EntityID
	private val entityIDValue = BoolValue("EntityID", true)
	private val entityIDLimitValue = IntegerValue("EntityIDLimit", 1000000000, 100000, 1000000000)

	// Static EntityID
	private val staticEntityIDValue = IntegerValue("StaticEntityIDs", 0, 0, 3)
	private val staticEntityID1 = IntegerValue("STaticEntityID-1", 99999999, 0, 1000000000)
	private val staticEntityID2 = IntegerValue("STaticEntityID-2", 0, 0, 1000000000)
	private val staticEntityID3 = IntegerValue("STaticEntityID-3", 0, 0, 1000000000)

	// NoColor
	private val colorValue = BoolValue("Color", false)

	// LivingTime (ticksExisted)
	private val livingTimeValue = BoolValue("LivingTime", false)
	private val livingTimeTicksValue = IntegerValue("LivingTimeTicks", 40, 1, 200)

	// Ground, Air
	private val groundValue = BoolValue("Ground", true)
	private val airValue = BoolValue("Air", false)
	private val invalidGroundValue = BoolValue("InvalidGround", true)

	// Swing
	private val swingValue = BoolValue("Swing", false)

	// Health
	private val healthValue = BoolValue("Health", false)

	// Invalid pitch (Derp)
	private val invalidPitchValue = BoolValue("Derp", true)

	// Was Invisible
	private val wasInvisibleValue = BoolValue("WasInvisible", false)

	// No Armor
	private val armorValue = BoolValue("Armor", false)

	// Zero Ping
	private val pingValue = BoolValue("Ping", false)

	// Needs to got damaged
	private val needHitValue = BoolValue("NeedHit", false)

	// Duplicate entity in the world
	private val duplicateInWorldValue = BoolValue("DuplicateInWorld", false)

	// Duplicate player in tab
	private val duplicateInTabValue = BoolValue("DuplicateInTab", false)
	private val duplicateInTabNameModeValue = ListValue("DuplicateInTabNameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName")
	private val duplicateInTabStripColorsValue = BoolValue("DuplicateInTabStripColorsInDisplayname", true)

	// Always In Radius
	private val alwaysInRadiusValue = BoolValue("AlwaysInRadius", false)
	private val alwaysRadiusValue = FloatValue("AlwaysInRadiusBlocks", 20f, 5f, 30f)

	// Teleport Packet
	private val teleportPacketValue = BoolValue("TeleportPacket", false)
	private val teleportThresholdDistance = FloatValue("TeleportPacketDistanceThreshold", 8.0f, 0.3125f, 16.0f)
	private val teleportPacketCountingSysValue = BoolValue("TeleportPacketCountingSystem", false)
	private val teleportPacketCountLimitValue = IntegerValue("TeleportPacketCountLimit", 15, 1, 40)
	private val teleportPacketCountDecremSysValue = BoolValue("TeleportPacketCountDecrementSystem", false)

	// (XZ) Speed
	private val speedValue = BoolValue("Speed", false)
	private val speedLimitValue = IntegerValue("SpeedLimit", 32, 1, 255)
	private val speedCountingSysValue = BoolValue("SpeedCountingSystem", true)
	private val speedCountLimitValue = IntegerValue("SpeedCountLimit", 5, 1, 10)
	private val speedCountDecremSysValue = BoolValue("SpeedCountDecrementSystem", false)

	// Y Speed
	private val ySpeedValue = BoolValue("YSpeed", false)
	private val ySpeedLimitValue = FloatValue("YSpeedLimit", 32.0f, 5.0f, 255.0f)
	private val ySpeedCountingSysValue = BoolValue("YSpeedCountingSystem", true)
	private val ySpeedCountLimitValue = IntegerValue("YSpeedCountLimit", 2, 1, 10)
	private val ySpeedCountDecremSysValue = BoolValue("YSpeedCountDecrementSystem", false)

	// Spawned position
	private val spawnedpositionValue = BoolValue("SpawnedPosition", true)

	// Position Consistency
	private val positionValue = BoolValue("PositionConsistency", true)

	private val positionBackValue = FloatValue("PositionBackDistance", 3.0f, 2.0f, 10.0f)
	private val positionYValue = FloatValue("PositionYDistance", 3.0f, 2.0f, 10.0f)

	private val positionBack2Value = FloatValue("PositionBackDistance2", 6.0f, 2.0f, 10.0f)
	private val positionY2Value = FloatValue("PositionYDistance2", 6.0f, 2.0f, 10.0f)

	// Position Consistency - Delta
	private val positionExpectationDeltaLimitValue = FloatValue("PositionConsistencyDeltaLimit", 1.0f, 0.1f, 3.0f)
	private val positionExpectationDeltaCountLimitValue = IntegerValue("PositionConsistencyDeltaCountLimit", 10, 1, 100)
	private val positionExpectationDeltaCountSysValue = BoolValue("PositionConsistencyDeltaCountDecrementSystem", false)

	// Position Consistency - Delta Consistency
	private val positionExpectationDeltaConsistencyValue = BoolValue("PositionConsistencyDeltaConsistencyCheck", false)
	private val positionRequiredExpectationDeltaToCheckConsistencyValue = FloatValue("PositionRequiredExpectationDeltaToCheckConsistency", 1.0f, 0.1f, 3.0f)
	private val positionExpectationDeltaConsistencyDeltaLimitValue = FloatValue("PositionExpectationDeltaConsistencyDeltaLimit", 0.1f, 0.0f, 0.5f)
	private val positionExpectationDeltaConsistencyCountLimitValue = IntegerValue("PositionExpectationDeltaConsistencyCountLimit", 10, 1, 100)
	private val positionExpectationDeltaConsistencyCountSysValue = BoolValue("PositionExpectationDeltaConsistencyCountDecrementSystem", false)

	// Custom Name
	private val customNameValue = BoolValue("EmptyCustomName", false)

	// '[NPC] ' prefix in name
	private val npcValue = BoolValue("NPC", false)

	private val bedWarsNPCValue = BoolValue("BedWarsNPC", false)
	private val gwenValue = BoolValue("GWEN", false)
	private val watchdogValue = BoolValue("Watchdog", false)
	private val watchdogRemoveValue = BoolValue("RemoveWatchdogBot", false)

	private val drawExpectedPosValue = BoolValue("MarkExpectedPosition", false)

	private val ground = mutableListOf<Int>()
	private val air = mutableListOf<Int>()
	private val invalidGround = mutableMapOf<Int, Int>()
	private val swing = mutableListOf<Int>()
	private val invisible = mutableListOf<Int>()
	private val hitted = mutableListOf<Int>()
	private val notAlwaysInRadius = mutableListOf<Int>()

	private val spawnedposition = mutableListOf<Int>()
	private val gwenBots = mutableListOf<Int>()
	private val watchdogBots = mutableListOf<Int>()

	private val xzspeed = mutableMapOf<Int, Int>()
	private val yspeed = mutableMapOf<Int, Int>()

	private val position_violation = mutableMapOf<Int, Int>()
	private val position_consistency_lastdistancedelta = mutableMapOf<Int, Double>()
	private val position_consistency_violation = mutableMapOf<Int, Int>()

	private val teleportpacket_violation = mutableMapOf<Int, Int>()
	private val removedBots = mutableListOf<Int>()

	private val lastRemoved = MSTimer()

	@JvmStatic
	fun checkTabList(targetName: String, displayName: Boolean, equals: Boolean, stripColors: Boolean): Boolean
	{
		for (networkPlayerInfo in mc.netHandler.playerInfoMap)
		{
			var networkName = networkPlayerInfo.getFullName(displayName)
			if (stripColors) networkName = stripColor(networkName)!!

			if (if (equals) targetName == networkName else targetName.contains(networkName)) return true
		}

		return false
	}

	@JvmStatic // TODO: Remove as soon EntityUtils is translated to kotlin
	fun isBot(entity: IEntityLivingBase): Boolean
	{ // Check if entity is a player
		if (!classProvider.isEntityPlayer(entity)) return false

		val displayName: String? = entity.displayName?.formattedText

		// Check if anti bot is enabled
		if (!state) return false

		// Anti Bot checks

		// NoColor
		if (colorValue.get() && !entity.displayName!!.formattedText.replace("\u00A7r", "").contains("\u00A7")) return true

		// LivingTime
		if (livingTimeValue.get() && entity.ticksExisted < livingTimeTicksValue.get()) return true

		// Ground
		if (groundValue.get() && !ground.contains(entity.entityId)) return true

		// Air
		if (airValue.get() && !air.contains(entity.entityId)) return true

		// Swing
		if (swingValue.get() && !swing.contains(entity.entityId)) return true

		// Health
		if (healthValue.get() && entity.health > 20F) return true

		// EntityID
		if (entityIDValue.get() && (entity.entityId >= entityIDLimitValue.get() || entity.entityId <= -1)) return true

		// StaticEntityID
		if (staticEntityIDValue.get() > 0)
		{
			val ids = arrayOf(staticEntityID1.get(), staticEntityID2.get(), staticEntityID3.get())
			if ((0 until staticEntityIDValue.get()).map { ids[it] }.any { entity.entityId == it }) return true
		}

		// Invalid pitch (Derp)
		if (invalidPitchValue.get() && (entity.rotationPitch > 90F || entity.rotationPitch < -90F)) return true

		// Was Invisible
		if (wasInvisibleValue.get() && invisible.contains(entity.entityId)) return true

		// Armor
		if (armorValue.get())
		{
			val player = entity.asEntityPlayer()

			if (player.inventory.armorInventory[0] == null && player.inventory.armorInventory[1] == null && player.inventory.armorInventory[2] == null && player.inventory.armorInventory[3] == null) return true
		}

		// Ping
		if (pingValue.get() && mc.netHandler.getPlayerInfo(entity.asEntityPlayer().uniqueID)?.responseTime == 0) return true

		// NeedHit
		if (needHitValue.get() && !hitted.contains(entity.entityId)) return true

		// Invalid-Ground
		if (invalidGroundValue.get() && invalidGround.getOrDefault(entity.entityId, 0) >= 10) return true

		// Tab
		if (tabValue.get())
		{
			val equals = tabModeValue.get().equals("Equals", ignoreCase = true)
			val displayNameMode: Boolean = tabNameModeValue.get().equals("DisplayName", ignoreCase = true)
			var targetName = if (displayNameMode) entity.displayName?.formattedText else entity.asEntityPlayer().gameProfile.name
			if (tabStripColorsValue.get()) targetName = stripColor(targetName)

			if (targetName != null && !checkTabList(targetName, displayNameMode, equals, tabStripColorsValue.get())) return true
		}

		// Duplicate in the world
		if (duplicateInWorldValue.get() && mc.theWorld!!.loadedEntityList.filter { classProvider.isEntityPlayer(it) && it.asEntityPlayer().displayNameString == it.asEntityPlayer().displayNameString }.size > 1) return true

		// Duplicate in the tab
		if (duplicateInTabValue.get() && mc.netHandler.playerInfoMap.filter {
				var entityName = entity.name
				if (duplicateInTabStripColorsValue.get()) entityName = stripColor(entityName)

				var itName = it.getFullName(duplicateInTabNameModeValue.get().equals("DisplayName", true))
				if (duplicateInTabStripColorsValue.get()) itName = stripColor(itName)!!

				entityName == itName
			}.size > 1) return true

		// Always in radius
		if (alwaysInRadiusValue.get() && !notAlwaysInRadius.contains(entity.entityId)) return true

		// XZ Speed
		if (speedValue.get() && xzspeed.containsKey(entity.entityId) && (!speedCountingSysValue.get() || xzspeed[entity.entityId]!! >= speedCountLimitValue.get())) return true

		// Y Speed
		if (ySpeedValue.get() && yspeed.containsKey(entity.entityId) && (!ySpeedCountingSysValue.get() || yspeed[entity.entityId]!! >= ySpeedCountLimitValue.get())) return true

		// Teleport Packet
		if (teleportPacketValue.get() && teleportpacket_violation.containsKey(entity.entityId) && (!teleportPacketCountingSysValue.get() || teleportpacket_violation[entity.entityId]!! >= teleportPacketCountLimitValue.get())) return true

		// Spawned Position
		if (spawnedpositionValue.get() && spawnedposition.contains(entity.entityId)) return true

		if (positionValue.get() && (position_violation.getOrDefault(entity.entityId, 0) >= positionExpectationDeltaCountLimitValue.get() || positionExpectationDeltaConsistencyValue.get() && position_consistency_violation.getOrDefault(
				entity.entityId, 0
			) >= positionExpectationDeltaConsistencyCountLimitValue.get())) return true

		if (customNameValue.get() && entity.customNameTag == "") return true

		if (npcValue.get() && (entity.displayName?.formattedText?.contains("\u00A78[NPC]") == true)) return true

		if (bedWarsNPCValue.get() && (displayName!!.isEmpty() || displayName[0] != '\u00A7') && displayName.endsWith("\u00A7r")) return true

		if (gwenValue.get() && gwenBots.contains(entity.entityId)) return true

		if (watchdogValue.get() && watchdogBots.contains(entity.entityId)) return true

		return entity.name!!.isEmpty() || entity.name == mc.thePlayer!!.name
	}

	override fun onDisable()
	{
		clearAll()
		super.onDisable()
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val packet = event.packet

		if (classProvider.isSPacketEntity(packet))
		{
			val packetEntity = packet.asSPacketEntity()
			val entity = packetEntity.getEntity(theWorld)

			if (classProvider.isEntityPlayer(entity) && entity != null)
			{
				val displayName: String? = entity.displayName?.formattedText
				val customName: String = entity.asEntityPlayer().customNameTag

				// Ground
				if (packetEntity.onGround && !ground.contains(entity.entityId)) ground.add(entity.entityId)

				// Air
				if (!packetEntity.onGround && !air.contains(entity.entityId)) air.add(entity.entityId)

				// Invalid-Ground
				if (packetEntity.onGround)
				{
					if (entity.prevPosY != entity.posY) invalidGround[entity.entityId] = invalidGround.getOrDefault(entity.entityId, 0) + 1
				}
				else
				{
					val currentVL = invalidGround.getOrDefault(entity.entityId, 0) / 2
					if (currentVL <= 0) invalidGround.remove(entity.entityId)
					else invalidGround[entity.entityId] = currentVL
				}

				// Was Invisible
				if (entity.invisible && !invisible.contains(entity.entityId)) invisible.add(entity.entityId)

				// Always in radius
				if (!notAlwaysInRadius.contains(entity.entityId) && thePlayer.getDistanceToEntity(entity) > alwaysRadiusValue.get()) notAlwaysInRadius.add(entity.entityId)

				if (hypot(abs(entity.prevPosX - entity.posX), abs(entity.prevPosZ - entity.posZ)) > speedLimitValue.get()) xzspeed[entity.entityId] = xzspeed.getOrDefault(entity.entityId, 0) + 1
				else if (speedCountDecremSysValue.get())
				{
					val currentVL: Int = xzspeed.getOrDefault(entity.entityId, 0) / 2
					if (currentVL <= 0) xzspeed.remove(entity.entityId) else xzspeed[entity.entityId] = currentVL
				}

				if (abs(entity.prevPosY - entity.posY) > ySpeedLimitValue.get()) yspeed[entity.entityId] = yspeed.getOrDefault(entity.entityId, 0) + 1
				else if (ySpeedCountDecremSysValue.get())
				{
					val currentVL = yspeed.getOrDefault(entity.entityId, 0) / 2
					if (currentVL <= 0) yspeed.remove(entity.entityId) else yspeed[entity.entityId] = currentVL
				}

				val yaw = if (RotationUtils.serverRotation != null) RotationUtils.serverRotation.yaw else thePlayer.rotationYaw
				val dir = WMathHelper.toRadians(yaw - 180.0F)

				val expectedX = thePlayer.posX - functions.sin(dir) * positionBackValue.get()
				val expectedY = thePlayer.posY + positionYValue.get()
				val expectedZ = thePlayer.posZ + functions.cos(dir) * positionBackValue.get()

				val expectedX2 = thePlayer.posX - functions.sin(dir) * positionBack2Value.get()
				val expectedY2 = thePlayer.posY + positionY2Value.get()
				val expectedZ2 = thePlayer.posZ + functions.cos(dir) * positionBack2Value.get()

				val distances = doubleArrayOf(entity.getDistance(expectedX, expectedY, expectedZ), entity.getDistance(expectedX2, expectedY2, expectedZ2))
				for (distance in distances)
				{
					// Position Delta
					if (distance <= positionExpectationDeltaLimitValue.get()) position_violation[entity.entityId] = position_violation.getOrDefault(entity.entityId, 0) + 1
					else if (positionExpectationDeltaCountSysValue.get())
					{
						val currentVL = position_violation.getOrDefault(entity.entityId, 0) / 2
						if (currentVL <= 0) position_violation.remove(entity.entityId) else position_violation[entity.entityId] = currentVL
					}

					// Position Delta Consistency
					if (distance <= positionRequiredExpectationDeltaToCheckConsistencyValue.get())
					{
						val lastdistance = position_consistency_lastdistancedelta.getOrDefault(entity.entityId, Double.MAX_VALUE)
						val consistency = abs(lastdistance - distance)
						if (consistency <= positionExpectationDeltaConsistencyDeltaLimitValue.get()) position_consistency_violation[entity.entityId] = position_consistency_violation.getOrDefault(entity.entityId, 0) + 1
						else if (positionExpectationDeltaConsistencyCountSysValue.get())
						{
							val currentVL = position_consistency_violation.getOrDefault(entity.entityId, 0) / 2
							if (currentVL <= 0) position_consistency_violation.remove(entity.entityId) else position_consistency_violation[entity.entityId] = currentVL
						}
						position_consistency_lastdistancedelta[entity.entityId] = distance
					}
					else
					{
						val currentVL = position_consistency_violation.getOrDefault(entity.entityId, 0) / 2
						if (currentVL <= 0) position_consistency_violation.remove(entity.entityId) else position_consistency_violation[entity.entityId] = currentVL
					}
				}

				// ticksExisted > 40 && custom name tag is empty = Mineplex GWEN bot
				if (thePlayer.ticksExisted > 40 && entity.asEntityPlayer().customNameTag == "" && !gwenBots.contains(entity.entityId)) gwenBots.add(entity.entityId)

				// invisible + display name isn't red but ends with color reset char (\u00A7r) + displayname equals customname + entity is near than 3 block horizontally + y delta between entity and player is 10~13 = Watchdog Bot
				if (entity.invisible && displayName?.startsWith("\u00A7c") == false && displayName.endsWith("\u00A7r") && displayName == customName)
				{
					val deltaX = abs(entity.posX - thePlayer.posX)
					val deltaY = abs(entity.posY - thePlayer.posY)
					val deltaZ = abs(entity.posZ - thePlayer.posZ)
					val horizontalDistance = hypot(deltaX, deltaZ)
					if (deltaY < 13 && deltaY > 10 && horizontalDistance < 3 && !checkTabList(entity.asEntityPlayer().gameProfile.name, displayName = false, equals = true, stripColors = true))
					{
						if (watchdogRemoveValue.get())
						{
							lastRemoved.reset()
							removedBots.add(entity.entityId)
							theWorld.removeEntityFromWorld(entity.entityId)
						}
						watchdogBots.add(entity.entityId)
					}
				}

				// invisible + custom name is red and contains color reset char (\u00A7r) = watchdog bot
				if (entity.invisible && customName.toLowerCase().contains("\u00A7c") && customName.toLowerCase().contains("\u00A7r"))
				{
					if (watchdogRemoveValue.get())
					{
						lastRemoved.reset()
						removedBots.add(entity.entityId)
						theWorld.removeEntityFromWorld(entity.entityId)
					}
					watchdogBots.add(entity.entityId)
				}

				// display name isn't red + custom name isn't empty = watchdog bot
				if (displayName?.contains("\u00A7c") == false && customName.isNotEmpty()) watchdogBots.add(entity.entityId)
			}
		}

		if (classProvider.isSPacketEntityTeleport(packet))
		{
			val packetEntityTeleport = packet.asSPacketEntityTeleport()
			val entity: IEntity? = theWorld.getEntityByID(packetEntityTeleport.entityId)

			if (entity != null && classProvider.isEntityPlayer(entity))
			{
				val dX: Double = packetEntityTeleport.x / 32.0
				val dY: Double = packetEntityTeleport.y / 32.0
				val dZ: Double = packetEntityTeleport.z / 32.0
				if (entity.asEntityPlayer().getDistanceSq(dX, dY, dZ) <= teleportThresholdDistance.get() * teleportThresholdDistance.get()) teleportpacket_violation[entity.entityId] = teleportpacket_violation.getOrDefault(entity.entityId, 0) + 1
				else if (teleportPacketCountDecremSysValue.get())
				{
					val currentVL = teleportpacket_violation.getOrDefault(entity.entityId, 0) / 2
					if (currentVL <= 0) teleportpacket_violation.remove(entity.entityId) else teleportpacket_violation[entity.entityId] = currentVL
				}
			}
		}

		if (classProvider.isSPacketAnimation(packet))
		{
			val packetAnimation = packet.asSPacketAnimation()
			val entity = theWorld.getEntityByID(packetAnimation.entityID)

			if (entity != null && classProvider.isEntityLivingBase(entity) && packetAnimation.animationType == 0 && !swing.contains(entity.entityId)) swing.add(entity.entityId)
		}

		if (classProvider.isSPacketSpawnPlayer(packet))
		{
			val packetPlayerSpawn = packet.asSPacketSpawnPlayer()
			val entityX: Double = packetPlayerSpawn.x.toDouble()
			val entityY: Double = packetPlayerSpawn.y.toDouble()
			val entityZ: Double = packetPlayerSpawn.z.toDouble()
			val deltaX = thePlayer.posX - entityX
			val deltaY = thePlayer.posY - entityY
			val deltaZ = thePlayer.posZ - entityZ
			val distance = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
			if (distance <= 18 && entityY > thePlayer.posY + 1.0 && thePlayer.posX != entityX && thePlayer.posY != entityY && thePlayer.posZ != entityZ) spawnedposition.add(packetPlayerSpawn.entityID)
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") e: Render3DEvent)
	{
		if (positionValue.get() && drawExpectedPosValue.get())
		{
			val thePlayer = mc.thePlayer ?: return

			val partialTicks = e.partialTicks

			val yaw = if (RotationUtils.serverRotation != null)
			{
				val serverYaw = RotationUtils.serverRotation.yaw
				if (RotationUtils.lastServerRotation != null)
				{
					val lastServerYaw = RotationUtils.lastServerRotation.yaw
					lastServerYaw + (serverYaw - lastServerYaw) * partialTicks
				}
				else RotationUtils.serverRotation.yaw
			}
			else
			{
				val rotYaw = thePlayer.rotationYaw
				val lastRotYaw = thePlayer.prevRotationYaw

				lastRotYaw + (rotYaw - lastRotYaw) * partialTicks
			}

			val dir = WMathHelper.toRadians(yaw - 180.0F)

			val back1 = positionBackValue.get()
			val y1 = positionYValue.get()

			val y2 = positionY2Value.get()
			val back2 = positionBack2Value.get()

			val sin = -functions.sin(dir)
			val cos = functions.cos(dir)

			val posX = thePlayer.lastTickPosX + (thePlayer.posX - thePlayer.lastTickPosX) * partialTicks
			val posY = thePlayer.lastTickPosY + (thePlayer.posY - thePlayer.lastTickPosY) * partialTicks
			val posZ = thePlayer.lastTickPosZ + (thePlayer.posZ - thePlayer.lastTickPosZ) * partialTicks

			val expectedX = posX + sin * back1
			val expectedY = posY + y1
			val expectedZ = posZ + cos * back1

			val expectedX2 = posX + sin * back2
			val expectedY2 = posY + y2
			val expectedZ2 = posZ + cos * back2

			val renderManager = mc.renderManager
			val renderPosX = renderManager.renderPosX
			val renderPosY = renderManager.renderPosY
			val renderPosZ = renderManager.renderPosZ

			val deltaLimit = positionExpectationDeltaLimitValue.get()

			val width = 0.3 + deltaLimit
			val height = 1.62 + deltaLimit
			RenderUtils.drawAxisAlignedBB(classProvider.createAxisAlignedBB(expectedX - width - renderPosX, expectedY - renderPosY, expectedZ - width - renderPosZ, expectedX + width - renderPosX, expectedY + height - renderPosY, expectedZ + width - renderPosZ), Color(255, 0, 0, 60))
			RenderUtils.drawAxisAlignedBB(classProvider.createAxisAlignedBB(expectedX2 - width - renderPosX, expectedY2 - deltaLimit - renderPosY, expectedZ2 - width - renderPosZ, expectedX2 + width - renderPosX, expectedY2 + height - renderPosY, expectedZ2 + width - renderPosZ), Color(0, 255, 0, 60))
		}
	}

	@EventTarget
	fun onAttack(e: AttackEvent)
	{
		val entity = e.targetEntity

		if (entity != null && classProvider.isEntityLivingBase(entity) && !hitted.contains(entity.entityId)) hitted.add(entity.entityId)
	}

	@EventTarget
	fun onWorld(@Suppress("UNUSED_PARAMETER") event: WorldEvent?)
	{
		clearAll()
	}

	private fun clearAll()
	{
		hitted.clear()
		swing.clear()
		ground.clear()
		invalidGround.clear()
		invisible.clear()
		notAlwaysInRadius.clear()

		spawnedposition.clear()
		gwenBots.clear()
		watchdogBots.clear()

		teleportpacket_violation.clear()

		xzspeed.clear()
		yspeed.clear()

		position_violation.clear()
		position_consistency_lastdistancedelta.clear()
		position_consistency_violation.clear()

		removedBots.clear()
	}
}
