package net.ccbluex.liquidbounce.features.module.modules.fun

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.Colors
import net.ccbluex.liquidbounce.utils.render.gl.GL11.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer

@ModuleInfo(name = "FakeHackers", description = "module.fakeHackers.description", category = Category.FUN)
object FakeHackers : Module() {

private val mode by choose("Mode", "KillAura", arrayOf("KillAura"))
private val range by float("Range", 6f, 1f..10f)

private val fakeHackers = mutableListOf<EntityPlayer>() // list of fake hackers
private val targets = mutableListOf<EntityPlayer>() // list of targets

val renderHandler = handler<Render3DEvent> { event ->
for (fakeHacker in fakeHackers) {
// find the closest target in range using maxByOrNull function
val target = targets.maxByOrNull { range - fakeHacker.getDistanceToEntityBox(it) }
?.takeIf { fakeHacker.getDistanceToEntityBox(it) <= range }

if (target != null) {
// draw a line from the fake hacker to the target
RenderUtils.drawLine(fakeHacker.posX, fakeHacker.posY + fakeHacker.eyeHeight, fakeHacker.posZ,
target.posX, target.posY + target.eyeHeight / 2, target.posZ, 2f, Colors.RED)

// calculate the rotations for the fake hacker to face the target using LiquidBounce API function
val rotations = LiquidBounce.combatManager.getRotationsToEntity(target)

// save the original rotations of the fake hacker using destructuring declaration
val (prevYaw, prevPitch) = fakeHacker.rotationYaw to fakeHacker.rotationPitch

// set the rotations of the fake hacker to face the target using named arguments
fakeHacker.setRotation(yaw = rotations.yaw, pitch = rotations.pitch)

// save the original state of OpenGL
GlStateManager.pushMatrix()
GlStateManager.disableCull()
GlStateManager.enablePolygonOffset()
GlStateManager.doPolygonOffset(1f, -1500000f)

// render the fake hacker with the new rotations using string template
mc.renderManager.renderEntityWithPosYaw(fakeHacker, "${event.partialTicks}")

// restore the original state of OpenGL
GlStateManager.doPolygonOffset(1f, 1500000f)
GlStateManager.disablePolygonOffset()
GlStateManager.enableCull()
GlStateManager.popMatrix()

// restore the original rotations of the fake hacker using infix notation
fakeHacker setRotation prevYaw to prevPitch
}
}
}
}

// use infix notation to simplify setRotation call
infix fun EntityPlayer.setRotation(rotations: Pair<Float, Float>) {
this.rotationYaw = rotations.first
this.rotationPitch = rotations.second
}