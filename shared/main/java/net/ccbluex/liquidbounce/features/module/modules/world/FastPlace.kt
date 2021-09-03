/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ValueGroup

@ModuleInfo(name = "FastPlace", description = "Allows you to place blocks faster.", category = ModuleCategory.WORLD)
class FastPlace : Module()
{
	val speedValue = IntegerValue("Speed", 0, 0, 4)

	private val inhTest1 = ValueGroup("Stage1")
	private val inhTest2 = ValueGroup("Stage2")
	private val inhTest3 = ValueGroup("Stage3")
	private val inhTest4 = ValueGroup("Stage4")
	private val inhTest5 = ValueGroup("Stage5")
	private val inhTest6 = ValueGroup("Stage6")
	private val inhTest7 = ValueGroup("Stage7")
	private val inhTest8 = BoolValue("DummyValue", false)

	init
	{
		inhTest7.add(inhTest8)
		inhTest6.add(inhTest7)
		inhTest5.add(inhTest6)
		inhTest4.add(inhTest5)
		inhTest3.add(inhTest4)
		inhTest2.add(inhTest3)
		inhTest1.add(inhTest2)
	}

	override val tag: String
		get() = "${speedValue.get()}"
}
