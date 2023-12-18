package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.script.bindings.features.JsSetting
import net.ccbluex.liquidbounce.script.bindings.globals.JsClient
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.util.Hand
import org.graalvm.polyglot.Value

/**
 * The main hub of the ScriptAPI that provides access to all kind of useful APIs.
 */
object JsApiProvider {

    internal fun setupUsefulContext(context: Value) = context.apply {
        // Class bindings
        // -> Client API
        putMember("Setting", JsSetting)
        putMember("CommandBuilder", CommandBuilder)
        putMember("ParameterBuilder", ParameterBuilder)
        // -> Minecraft API
        // todo: test if this works
        putMember("Hand", Hand::class.java)

        // Variable bindings
        putMember("mc", mc)
        putMember("client", JsClient)
        putMember("api", JsApiProvider)
    }

    /**
     * A collection of useful rotation utilities for the ScriptAPI.
     * This SHOULD not be changed in a way that breaks backwards compatibility.
     *
     * This is a singleton object, so it can be accessed from the script API like this:
     * ```js
     * api.rotationUtil.newRaytracedRotationEntity(entity, 4.2, 0.0)
     * api.rotationUtil.newRotationEntity(entity)
     * api.rotationUtil.aimAtRotation(rotation, true)
     * ```
     */
    @JvmField
    val rotationUtil = JsRotationUtil

    /**
     * Object used by the script API to provide an idiomatic way of creating module values.
     */
    @JvmField
    val itemUtil = JsItemUtil

    @JvmField
    val networkUtil = JsNetworkUtil

    @JvmField
    val interactionUtil = JsInteractionUtil

    @JvmField
    val blockUtil = JsBlockUtil

}
