/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK
import net.minecraft.util.Vec3
import kotlin.math.ceil
import kotlin.math.roundToInt

object TeleportCommand : Command("tp", "teleport") {
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>) {
		if (args.size !in 4..5 ) {
			chatSyntax("tp <x> <y> <z> [maxDistancePerPacket = 5]")
			return
		}

		val (x, y, z) = args.drop(1).map { it.toDoubleOrNull() }

		val maxDistancePerPacket = args.getOrNull(4)?.toDoubleOrNull() ?: 5.0

		if (x == null || y == null || z == null) {
			chatSyntax("tp <x> <y> <z> [maxDistancePerPacket = 5]")
			return
		}

		val moveVec = Vec3(x, y, z) - mc.thePlayer.positionVector

		val packetsNeeded = ceil(moveVec.lengthVector() / maxDistancePerPacket).toInt()

		repeat(packetsNeeded) {
			val ratio = it / packetsNeeded.toDouble()

			val vec = mc.thePlayer.positionVector + moveVec * ratio

			val (pathX, pathY, pathZ) = vec

			if (it == packetsNeeded - 1)
				mc.thePlayer.setPositionAndUpdate(x, y, z)
			else sendPacket(C04PacketPlayerPosition(pathX, pathY, pathZ, false))
		}

		chat("Teleported to §a$x $y $z§3.")
	}

	override fun tabComplete(args: Array<String>): List<String> {
		// TODO: Should try to check for collisions by offsetting player's collision box instead
		val rayTrace = mc.thePlayer.rayTrace(500.0, 1f)

		if (rayTrace == null || rayTrace.typeOfHit != BLOCK)
			return emptyList()

		val (x, y, z) = rayTrace.blockPos ?: return emptyList()

		val suggestion = when (args.size) {
			1 -> x
			2 -> y + 1
			3 -> z
			else -> return emptyList()
		}.toString()

		return if (suggestion.startsWith(args.last()))
			listOf(suggestion)
		else
			emptyList()
	}
}