/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script.api

import jdk.nashorn.api.scripting.JSObject
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.value.Value

@Suppress("unused")
@ModuleInfo(name = "ScriptModule", description = "Empty", category = ModuleCategory.MISC)
class ScriptModule(private val moduleObject: JSObject) : Module() {

    private val events = HashMap<String, JSObject>()
    private val _values = LinkedHashMap<String, Value<*>>()
    private var _tag: String? = null

    /**
     * Allows the user to access values by typing module.settings.<valuename>
     */
    val settings by lazy { _values }

    init {
        name = moduleObject.getMember("name") as String
        description = moduleObject.getMember("description") as String

        val categoryString = moduleObject.getMember("category") as String
        for (category in ModuleCategory.values())
            if (categoryString.equals(category.displayName, true))
                this.category = category

        if (moduleObject.hasMember("settings")) {
            val settings = moduleObject.getMember("settings") as JSObject

            for (settingName in settings.keySet())
                _values[settingName] = settings.getMember(settingName) as Value<*>
        }

        if (moduleObject.hasMember("tag"))
            _tag = moduleObject.getMember("tag") as String
    }

    override val values: List<Value<*>>
        get() {
            return _values.values.toList()
        }

    override var tag: String?
        get() = _tag
        set(value) {
            _tag = value
        }

    /**
     * Called from inside the script to register a new event handler.
     * @param eventName Name of the event.
     * @param handler JavaScript function used to handle the event.
     */
    fun on(eventName: String, handler: JSObject) {
        events[eventName] = handler
    }

    override fun onEnable() = callEvent("enable")

    override fun onDisable() = callEvent("disable")

    @EventTarget
    fun onUpdate(updateEvent: UpdateEvent) = callEvent("update")

    @EventTarget
    fun onMotion(motionEvent: MotionEvent) = callEvent("motion", motionEvent)

    @EventTarget
    fun onRender2D(render2DEvent: Render2DEvent) = callEvent("render2D", render2DEvent)

    @EventTarget
    fun onRender3D(render3DEvent: Render3DEvent) = callEvent("render3D", render3DEvent)

    @EventTarget
    fun onPacket(packetEvent: PacketEvent) = callEvent("packet", packetEvent)

    @EventTarget
    fun onJump(jumpEvent: JumpEvent) = callEvent("jump", jumpEvent)

    @EventTarget
    fun onAttack(attackEvent: AttackEvent) = callEvent("attack", attackEvent)

    @EventTarget
    fun onKey(keyEvent: KeyEvent) = callEvent("key", keyEvent)

    @EventTarget
    fun onMove(moveEvent: MoveEvent) = callEvent("move", moveEvent)

    @EventTarget
    fun onStep(stepEvent: StepEvent) = callEvent("step", stepEvent)

    @EventTarget
    fun onStepConfirm(stepConfirmEvent: StepConfirmEvent) = callEvent("stepConfirm")

    @EventTarget
    fun onWorld(worldEvent: WorldEvent) = callEvent("world", worldEvent)

    @EventTarget
    fun onSession(sessionEvent: SessionEvent) = callEvent("session")

    @EventTarget
    fun onClickBlock(clickBlockEvent: ClickBlockEvent) = callEvent("clickBlock", clickBlockEvent)

    @EventTarget
    fun onStrafe(strafeEvent: StrafeEvent) = callEvent("strafe", strafeEvent)

    @EventTarget
    fun onSlowDown(slowDownEvent: SlowDownEvent) = callEvent("slowDown", slowDownEvent)

    @EventTarget
    fun onShutdown(shutdownEvent: ClientShutdownEvent) = callEvent("shutdown")

    /**
     * Calls the handler of a registered event.
     * @param eventName Name of the event to be called.
     * @param payload Event data passed to the handler function.
     */
    private fun callEvent(eventName: String, payload: Any? = null) {
        try {
            events[eventName]?.call(moduleObject, payload)
        } catch (throwable: Throwable) {
            ClientUtils.getLogger().error("[ScriptAPI] Exception in module '$name'!", throwable)
        }
    }
}