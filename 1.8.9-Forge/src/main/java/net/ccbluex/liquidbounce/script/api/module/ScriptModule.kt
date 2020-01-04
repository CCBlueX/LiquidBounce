package net.ccbluex.liquidbounce.script.api.module

import jdk.nashorn.api.scripting.ScriptObjectMirror
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.value.Value

/**
 * Default Module Info of script module
 *
 * Name: ScriptModule
 * Description: Empty
 * Category: Misc
 */
@ModuleInfo(name = "ScriptModule", description = "Empty", category = ModuleCategory.MISC)

/**
 * A script module
 *
 * Script Support
 * @author CCBlueX
 */
class ScriptModule(private val scriptObjectMirror: ScriptObjectMirror) : Module() {

    private val _values = mutableListOf<Value<*>>()

    /**
     * Initialize a new script module
     *
     * Call getName function and set as name
     * Call getDescription function and set as description
     * Call getCategory function and search for enum and set as category
     */
    init {
        name = scriptObjectMirror.callMember("getName") as String
        description = scriptObjectMirror.callMember("getDescription") as String

        if (scriptObjectMirror.hasMember("addValues")) {
            val adaptedValues = mutableListOf<AdaptedValue>()
            scriptObjectMirror.callMember("addValues", adaptedValues)

            for (value in adaptedValues)
                _values.add(value.getValue())
        }

        val categoryString: String = scriptObjectMirror.callMember("getCategory") as String
        for (category in ModuleCategory.values())
            if (categoryString.equals(category.displayName, true))
                this.category = category
    }

    override val values: List<Value<*>>
        get() {
            return _values
        }

    override fun getValue(valueName: String) = _values.find { it.name.equals(valueName, ignoreCase = true) }

    override val tag: String?
        get() {
            return if (scriptObjectMirror.hasMember("getTag"))
                scriptObjectMirror.callMember("getTag") as String
            else null
        }

    /**
     * Handle onEnable and call js function of method
     */
    override fun onEnable() = call("onEnable")

    /**
     * Handle onDisable and call js function of method
     */
    override fun onDisable() = call("onDisable")

    /**
     * Handle onUpdate and call js function of method
     */
    @EventTarget
    fun onUpdate(updateEvent: UpdateEvent) = call("onUpdate")

    /**
     * Handle onMotion and call js function of method
     */
    @EventTarget
    fun onMotion(motionEvent: MotionEvent) = call("onMotion", motionEvent)

    /**
     * Handle onRender2D and call js function of method
     */
    @EventTarget
    fun onRender2D(render2DEvent: Render2DEvent) = call("onRender2D", render2DEvent)

    /**
     * Handle onRender3D and call js function of method
     */
    @EventTarget
    fun onRender3D(render3DEvent: Render3DEvent) = call("onRender3D", render3DEvent)

    /**
     * Handle onPacket and call js function of method
     */
    @EventTarget
    fun onPacket(packetEvent: PacketEvent) = call("onPacket", packetEvent)

    /**
     * Handle onJump and call js function of method
     */
    @EventTarget
    fun onJump(jumpEvent: JumpEvent) = call("onJump", jumpEvent)

    /**
     * Handle onAttack and call js function of method
     */
    @EventTarget
    fun onAttack(attackEvent: AttackEvent) = call("onAttack", attackEvent)

    /**
     * Handle onKey and call js function of method
     */
    @EventTarget
    fun onKey(keyEvent: KeyEvent) = call("onKey", keyEvent)

    /**
     * Handle onMove and call js function of method
     */
    @EventTarget
    fun onMove(moveEvent: MoveEvent) = call("onMove", moveEvent)

    /**
     * Handle onStep and call js function of method
     */
    @EventTarget
    fun onStep(stepEvent: StepEvent) = call("onStep", stepEvent)

    /**
     * Handle onStepConfirm and call js function of method
     */
    @EventTarget
    fun onStepConfirm(stepConfirmEvent: StepConfirmEvent) = call("onStepConfirm")

    /**
     * Handle onWorld and call js function of method
     */
    @EventTarget
    fun onWorld(worldEvent: WorldEvent) = call("onWorld", worldEvent)

    /**
     * Handle onSession and call js function of method
     */
    @EventTarget
    fun onSession(sessionEvent: SessionEvent) = call("onSession")

    /**
     * Call member of script when member is available
     */
    private fun call(member: String, event: Any? = null) {
        if (scriptObjectMirror.hasMember(member)) {
            try {
                scriptObjectMirror.callMember(member, event)
            } catch (throwable: Throwable) {
                ClientUtils.getLogger().error("An error occurred inside script module: ${name}", throwable)
            }
        }
    }

}
