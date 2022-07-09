package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.FloatValue

@ModuleInfo(name = "ItemPhysics", description = "Integrated ItemPhysics-lite mod.", category = ModuleCategory.RENDER)
class ItemPhysics : Module()
{
	var tick: Long = 0

	val itemRotationSpeed = FloatValue("RotateSpeed", 1.0f, 0.0f, 10.0f)

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		tick = System.nanoTime()
	}

	override val tag: String
		get() = "${itemRotationSpeed.get()}"
}
