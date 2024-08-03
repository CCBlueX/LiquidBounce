package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.MinecraftInstance

open class FlyMode(val modeName: String): MinecraftInstance() {
	open fun onMove(event: MoveEvent) {}
	open fun onPacket(event: PacketEvent) {}
	open fun onRender3D(event: Render3DEvent) {}
	open fun onBB(event: BlockBBEvent) {}
	open fun onJump(event: JumpEvent) {}
	open fun onStep(event: StepEvent) {}
	open fun onMotion(event: MotionEvent) {}
	open fun onUpdate() {}
	open fun onTick () {}

	open fun onEnable() {}
	open fun onDisable() {}
}