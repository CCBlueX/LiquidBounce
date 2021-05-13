/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import java.awt.Color
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

@ModuleInfo(name = "AntiBot", description = "Prevents KillAura from attacking AntiCheat bots.", category = ModuleCategory.MISC)
object AntiBot : Module()
{
	private val notificationValue = BoolValue("DetectionNotification", false)

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
	private val staticEntityID1 = IntegerValue("StaticEntityID-1", 99999999, 0, Int.MAX_VALUE)
	private val staticEntityID2 = IntegerValue("StaticEntityID-2", 999999999, 0, Int.MAX_VALUE)
	private val staticEntityID3 = IntegerValue("StaticEntityID-3", -1337, 0, Int.MAX_VALUE)

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

	private val drawExpectedPosValue = BoolValue("MarkExpectedPosition", false)

	// .contains() performance -> Set >>> List

	private val ground = mutableSetOf<Int>()
	private val air = mutableSetOf<Int>()
	private val swing = mutableSetOf<Int>()
	private val invisible = mutableSetOf<Int>()
	private val hitted = mutableSetOf<Int>()
	private val notAlwaysInRadius = mutableSetOf<Int>()
	private val spawnedPosition = mutableSetOf<Int>()
	private val gwen = mutableSetOf<Int>()
	private val watchdog = mutableSetOf<Int>()

	private val invalidGround = mutableMapOf<Int, Int>()
	private val hspeed = mutableMapOf<Int, Int>()
	private val vspeed = mutableMapOf<Int, Int>()
	private val positionVL = mutableMapOf<Int, Int>()
	private val positionConsistencyLastDistanceDelta = mutableMapOf<Int, Double>()
	private val positionConsistencyVL = mutableMapOf<Int, Int>()
	private val teleportpacket_violation = mutableMapOf<Int, Int>()

	@JvmStatic
	fun checkTabList(targetName: String, displayName: Boolean, equals: Boolean, stripColors: Boolean): Boolean = mc.netHandler.playerInfoMap.map { networkPlayerInfo ->
		var networkName = networkPlayerInfo.getFullName(displayName)

		if (stripColors) networkName = stripColor(networkName)

		networkName
	}.any { networkName -> if (equals) targetName == networkName else targetName.contains(networkName) }

	fun isBot(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, entity: IEntityLivingBase): Boolean
	{
		// Check if entity is a player
		val provider = classProvider

		if (!provider.isEntityPlayer(entity)) return false

		val displayName: String = entity.displayName.formattedText

		// Check if anti bot is enabled
		if (!state) return false

		// Anti Bot checks

		// NoColor
		if (colorValue.get() && !displayName.replace("\u00A7r", "").contains("\u00A7")) return true

		// LivingTime
		if (livingTimeValue.get() && entity.ticksExisted < livingTimeTicksValue.get()) return true

		val entityID = entity.entityId

		// Ground
		if (groundValue.get() && !ground.contains(entityID)) return true

		// Air
		if (airValue.get() && !air.contains(entityID)) return true

		// Swing
		if (swingValue.get() && !swing.contains(entityID)) return true

		// Health
		if (healthValue.get() && entity.health > 20F) return true

		// EntityID
		if (entityIDValue.get() && (entityID >= entityIDLimitValue.get() || entityID <= -1)) return true

		// StaticEntityID
		if (staticEntityIDValue.get() > 0)
		{
			val ids = arrayOf(staticEntityID1.get(), staticEntityID2.get(), staticEntityID3.get())
			if ((0 until staticEntityIDValue.get()).map(ids::get).any { entityID == it }) return true
		}

		// Invalid pitch (Derp)
		if (invalidPitchValue.get() && (entity.rotationPitch > 90F || entity.rotationPitch < -90F)) return true

		// Was Invisible
		if (wasInvisibleValue.get() && invisible.contains(entityID)) return true

		// Armor
		if (armorValue.get() && entity.asEntityPlayer().inventory.armorInventory.all { it == null }) return true

		val netHandler = mc.netHandler

		// Ping
		if (pingValue.get() && netHandler.getPlayerInfo(entity.asEntityPlayer().uniqueID)?.responseTime == 0) return true

		// NeedHit
		if (needHitValue.get() && !hitted.contains(entityID)) return true

		// Invalid-Ground
		if (invalidGroundValue.get() && invalidGround.getOrDefault(entityID, 0) >= 10) return true

		// Tab
		if (tabValue.get())
		{
			val equals = tabModeValue.get().equals("Equals", ignoreCase = true)
			val displayNameMode: Boolean = tabNameModeValue.get().equals("DisplayName", ignoreCase = true)
			var targetName = if (displayNameMode) displayName else entity.asEntityPlayer().gameProfile.name

			if (targetName != null)
			{
				if (tabStripColorsValue.get()) targetName = stripColor(targetName)

				if (!checkTabList(targetName, displayNameMode, equals, tabStripColorsValue.get())) return true
			}
		}

		// Duplicate in the world
		if (duplicateInWorldValue.get() && theWorld.loadedEntityList.count { provider.isEntityPlayer(it) && it.asEntityPlayer().displayNameString == it.asEntityPlayer().displayNameString } > 1) return true

		// Duplicate in the tab
		if (duplicateInTabValue.get() && netHandler.playerInfoMap.count {
				var entityName = entity.name

				if (duplicateInTabStripColorsValue.get()) entityName = stripColor(entityName)

				var itName = it.getFullName(duplicateInTabNameModeValue.get().equals("DisplayName", true))

				if (duplicateInTabStripColorsValue.get()) itName = stripColor(itName)

				entityName == itName
			} > 1) return true

		// Always in radius
		if (alwaysInRadiusValue.get() && !notAlwaysInRadius.contains(entityID)) return true

		// XZ Speed
		if (speedValue.get() && hspeed.containsKey(entityID) && (!speedCountingSysValue.get() || hspeed[entityID] ?: 0 >= speedCountLimitValue.get())) return true

		// Y Speed
		if (ySpeedValue.get() && vspeed.containsKey(entityID) && (!ySpeedCountingSysValue.get() || vspeed[entityID] ?: 0 >= ySpeedCountLimitValue.get())) return true

		// Teleport Packet
		if (teleportPacketValue.get() && teleportpacket_violation.containsKey(entityID) && (!teleportPacketCountingSysValue.get() || teleportpacket_violation[entityID] ?: 0 >= teleportPacketCountLimitValue.get())) return true

		// Spawned Position
		if (spawnedpositionValue.get() && spawnedPosition.contains(entityID)) return true

		if (positionValue.get() && (positionVL.getOrDefault(entityID, 0) >= positionExpectationDeltaCountLimitValue.get() || positionExpectationDeltaConsistencyValue.get() && positionConsistencyVL.getOrDefault(entityID, 0) >= positionExpectationDeltaConsistencyCountLimitValue.get())) return true

		if (customNameValue.get() && entity.customNameTag == "") return true

		if (npcValue.get() && displayName.contains("\u00A78[NPC]")) return true

		if (bedWarsNPCValue.get() && (displayName.isEmpty() || displayName[0] != '\u00A7') && displayName.endsWith("\u00A7r")) return true

		if (gwenValue.get() && gwen.contains(entityID)) return true

		if (watchdogValue.get() && watchdog.contains(entityID)) return true

		return entity.name.isEmpty() || entity.name == thePlayer.name
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

		val notify = notificationValue.get()

		val packet = event.packet

		val provider = classProvider

		if (provider.isSPacketEntity(packet))
		{
			val packetEntity = packet.asSPacketEntity()
			val entity = packetEntity.getEntity(theWorld)

			if (entity != null && provider.isEntityPlayer(entity))
			{
				val displayName: String = entity.displayName.formattedText
				val customName: String = entity.asEntityPlayer().customNameTag

				// Ground
				if (packetEntity.onGround && !ground.contains(entity.entityId)) ground.add(entity.entityId)

				// Air
				if (!packetEntity.onGround && !air.contains(entity.entityId)) air.add(entity.entityId)

				// Invalid-Ground
				if (packetEntity.onGround)
				{
					if (notify && invalidGroundValue.get()) notification("InvalidGround", "Suspicious onGround flag: $displayName")

					if (entity.prevPosY != entity.posY) invalidGround[entity.entityId] = invalidGround.getOrDefault(entity.entityId, 0) + 1
				}
				else
				{
					val currentVL = invalidGround.getOrDefault(entity.entityId, 0) shr 1
					if (currentVL <= 0) invalidGround.remove(entity.entityId)
					else invalidGround[entity.entityId] = currentVL
				}

				// Was Invisible
				if (entity.invisible && !invisible.contains(entity.entityId)) invisible.add(entity.entityId)

				// Always in radius
				if (!notAlwaysInRadius.contains(entity.entityId) && thePlayer.getDistanceToEntity(entity) > alwaysRadiusValue.get()) notAlwaysInRadius.add(entity.entityId)

				val hspeed = hypot(abs(entity.prevPosX - entity.posX), abs(entity.prevPosZ - entity.posZ))
				if (hspeed > speedLimitValue.get())
				{
					if (notify && speedValue.get()) notification("HSpeed", "Moved too fast (horizontally): $displayName ($hspeed blocks/tick)")
					this.hspeed[entity.entityId] = this.hspeed.getOrDefault(entity.entityId, 0) + 1
				}
				else if (speedCountDecremSysValue.get())
				{
					val currentVL: Int = this.hspeed.getOrDefault(entity.entityId, 0) shr 1
					if (currentVL <= 0) this.hspeed.remove(entity.entityId) else this.hspeed[entity.entityId] = currentVL
				}

				val vspeed = abs(entity.prevPosY - entity.posY)
				if (vspeed > ySpeedLimitValue.get())
				{
					if (notify && ySpeedValue.get()) notification("VSpeed", "Moved too fast (vertically): $displayName ($vspeed blocks/tick)")
					this.vspeed[entity.entityId] = this.vspeed.getOrDefault(entity.entityId, 0) + 1
				}
				else if (ySpeedCountDecremSysValue.get())
				{
					val currentVL = this.vspeed.getOrDefault(entity.entityId, 0) shr 1
					if (currentVL <= 0) this.vspeed.remove(entity.entityId) else this.vspeed[entity.entityId] = currentVL
				}

				val yaw = RotationUtils.serverRotation.yaw
				val dir = WMathHelper.toRadians(yaw - 180.0F)

				val func = functions

				val expectedX = thePlayer.posX - func.sin(dir) * positionBackValue.get()
				val expectedY = thePlayer.posY + positionYValue.get()
				val expectedZ = thePlayer.posZ + func.cos(dir) * positionBackValue.get()

				val expectedX2 = thePlayer.posX - func.sin(dir) * positionBack2Value.get()
				val expectedY2 = thePlayer.posY + positionY2Value.get()
				val expectedZ2 = thePlayer.posZ + func.cos(dir) * positionBack2Value.get()

				val positionExpectationDeltaLimit = positionExpectationDeltaLimitValue.get()
				val positionExpectationDeltaCountSys = positionExpectationDeltaCountSysValue.get()
				val positionRequiredExpectationDeltaToCheckConsistency = positionRequiredExpectationDeltaToCheckConsistencyValue.get()
				val positionExpectationDeltaConsistencyDeltaLimit = positionExpectationDeltaConsistencyDeltaLimitValue.get()
				val positionExpectationDeltaConsistencyCountSys = positionExpectationDeltaConsistencyCountSysValue.get()

				val distances = doubleArrayOf(entity.getDistance(expectedX, expectedY, expectedZ), entity.getDistance(expectedX2, expectedY2, expectedZ2))

				for (distance in distances)
				{
					// Position Delta
					val prevVL = positionVL.getOrDefault(entity.entityId, 0)
					if (distance <= positionExpectationDeltaLimit)
					{
						if (notify && positionValue.get() && (prevVL + 2) % 10 == 0) notification("Position", "Suspicious position: $displayName (VL: $prevVL)")
						positionVL[entity.entityId] = prevVL + 1
					}
					else if (positionExpectationDeltaCountSys)
					{
						val currentVL = prevVL shr 1
						if (currentVL <= 0) positionVL.remove(entity.entityId) else positionVL[entity.entityId] = currentVL
					}

					val prevConsistencyVL = positionConsistencyVL.getOrDefault(entity.entityId, 0)

					// Position Delta Consistency
					if (distance <= positionRequiredExpectationDeltaToCheckConsistency)
					{
						val lastdistance = positionConsistencyLastDistanceDelta.getOrDefault(entity.entityId, Double.MAX_VALUE)
						val consistency = abs(lastdistance - distance)

						if (consistency <= positionExpectationDeltaConsistencyDeltaLimit)
						{
							if (notify && positionExpectationDeltaConsistencyValue.get() && (prevConsistencyVL + 2) % 5 == 0) notification("Position-Consistency", "Suspicious position consistency: $displayName (posVL: $prevVL, posConsistencyVL: $prevConsistencyVL)")
							positionConsistencyVL[entity.entityId] = prevConsistencyVL + 1
						}
						else if (positionExpectationDeltaConsistencyCountSys)
						{
							val currentVL = prevConsistencyVL shr 1
							if (currentVL <= 0) positionConsistencyVL.remove(entity.entityId) else positionConsistencyVL[entity.entityId] = currentVL
						}

						positionConsistencyLastDistanceDelta[entity.entityId] = distance
					}
					else
					{
						val currentVL = prevConsistencyVL shr 1

						if (currentVL <= 0) positionConsistencyVL.remove(entity.entityId) else positionConsistencyVL[entity.entityId] = currentVL
					}
				}

				// ticksExisted > 40 && custom name tag is empty = Mineplex GWEN bot
				if (thePlayer.ticksExisted > 40 && entity.asEntityPlayer().customNameTag == "" && !gwen.contains(entity.entityId)) gwen.add(entity.entityId)

				// invisible + display name isn't red but ends with color reset char (\u00A7r) + displayname equals customname + entity is near than 3 block horizontally + y delta between entity and player is 10~13 = Watchdog Bot
				if (entity.invisible && !displayName.startsWith("\u00A7c") && displayName.endsWith("\u00A7r") && displayName == customName)
				{
					val deltaX = abs(entity.posX - thePlayer.posX)
					val deltaY = abs(entity.posY - thePlayer.posY)
					val deltaZ = abs(entity.posZ - thePlayer.posZ)
					val hDist = hypot(deltaX, deltaZ)
					if (deltaY < 13 && deltaY > 10 && hDist < 3 && !checkTabList(entity.asEntityPlayer().gameProfile.name, displayName = false, equals = true, stripColors = true))
					{
						if (notify && watchdogValue.get()) notification("Watchdog", "Detected watchdog bot: $displayName (hDist: $hDist, vDist: $deltaY)")
						watchdog.add(entity.entityId)
					}
				}

				// invisible + custom name is red and contains color reset char (\u00A7r) = watchdog bot
				if (entity.invisible && customName.toLowerCase().contains("\u00A7c") && customName.toLowerCase().contains("\u00A7r"))
				{
					if (notify && watchdogValue.get()) notification("Watchdog", "Detected watchdog bot: $displayName")
					watchdog.add(entity.entityId)
				}

				// display name isn't red + custom name isn't empty = watchdog bot
				if (!displayName.contains("\u00A7c") && customName.isNotEmpty())
				{
					if (notify && watchdogValue.get()) notification("Watchdog", "Detected watchdog bot: $displayName (customName: $customName)")
					watchdog.add(entity.entityId)
				}
			}
		}



		if (provider.isSPacketEntityTeleport(packet))
		{
			val packetEntityTeleport = packet.asSPacketEntityTeleport()
			val entity: IEntity? = theWorld.getEntityByID(packetEntityTeleport.entityId)

			if (entity != null && provider.isEntityPlayer(entity))
			{
				val dX: Double = packetEntityTeleport.x
				val dY: Double = packetEntityTeleport.y
				val dZ: Double = packetEntityTeleport.z
				val distSq = entity.asEntityPlayer().getDistanceSq(dX, dY, dZ)
				val distThreshold = teleportThresholdDistance.get()
				if (distSq <= distThreshold * distThreshold)
				{
					if (notify && teleportPacketValue.get()) notification("TeleportPacket", "Suspicious SPacketEntityTeleport: ${entity.displayName.formattedText} (dist: ${sqrt(distSq)})")
					teleportpacket_violation[entity.entityId] = teleportpacket_violation.getOrDefault(entity.entityId, 0) + 1
				}
				else if (teleportPacketCountDecremSysValue.get())
				{
					val currentVL = teleportpacket_violation.getOrDefault(entity.entityId, 0) shr 1
					if (currentVL <= 0) teleportpacket_violation.remove(entity.entityId) else teleportpacket_violation[entity.entityId] = currentVL
				}
			}
		}

		if (provider.isSPacketAnimation(packet))
		{
			val packetAnimation = packet.asSPacketAnimation()
			val entity = theWorld.getEntityByID(packetAnimation.entityID)

			if (entity != null && provider.isEntityLivingBase(entity) && packetAnimation.animationType == 0 && !swing.contains(entity.entityId)) swing.add(entity.entityId)
		}

		if (provider.isSPacketSpawnPlayer(packet))
		{
			val packetPlayerSpawn = packet.asSPacketSpawnPlayer()
			val entityX: Double = packetPlayerSpawn.x.toDouble()
			val entityY: Double = packetPlayerSpawn.y.toDouble()
			val entityZ: Double = packetPlayerSpawn.z.toDouble()
			val deltaX = thePlayer.posX - entityX
			val deltaY = thePlayer.posY - entityY
			val deltaZ = thePlayer.posZ - entityZ
			val distance = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
			if (distance <= 18 && entityY > thePlayer.posY + 1.0 && thePlayer.posX != entityX && thePlayer.posY != entityY && thePlayer.posZ != entityZ)
			{
				val entityID = packetPlayerSpawn.entityID

				if (notify && spawnedpositionValue.get()) notification("Spawn", "Suspicious spawn: Entity #$entityID (dist: $distance, vDist: ${abs(deltaY)})")
				spawnedPosition.add(entityID)
			}
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") e: Render3DEvent)
	{
		if (positionValue.get() && drawExpectedPosValue.get())
		{
			val thePlayer = mc.thePlayer ?: return

			val partialTicks = e.partialTicks

			val yaw = run {
				val serverYaw = RotationUtils.serverRotation.yaw
				val lastServerYaw = RotationUtils.lastServerRotation.yaw

				lastServerYaw + (serverYaw - lastServerYaw) * partialTicks
			}

			val dir = WMathHelper.toRadians(yaw - 180.0F)

			val back1 = positionBackValue.get()
			val y1 = positionYValue.get()

			val y2 = positionY2Value.get()
			val back2 = positionBack2Value.get()

			val func = functions

			val sin = -func.sin(dir)
			val cos = func.cos(dir)

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

			val provider = classProvider

			RenderUtils.drawAxisAlignedBB(provider.createAxisAlignedBB(expectedX - width - renderPosX, expectedY - renderPosY, expectedZ - width - renderPosZ, expectedX + width - renderPosX, expectedY + height - renderPosY, expectedZ + width - renderPosZ), Color(255, 0, 0, 60))
			RenderUtils.drawAxisAlignedBB(provider.createAxisAlignedBB(expectedX2 - width - renderPosX, expectedY2 - deltaLimit - renderPosY, expectedZ2 - width - renderPosZ, expectedX2 + width - renderPosX, expectedY2 + height - renderPosY, expectedZ2 + width - renderPosZ), Color(0, 255, 0, 60))
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

		spawnedPosition.clear()
		gwen.clear()
		watchdog.clear()

		teleportpacket_violation.clear()

		hspeed.clear()
		vspeed.clear()

		positionVL.clear()
		positionConsistencyLastDistanceDelta.clear()
		positionConsistencyVL.clear()
	}

	private fun notification(checkName: String, message: String)
	{
		LiquidBounce.hud.addNotification("AntiBot: $checkName check", message, 1000L, Color.red)
	}
}
