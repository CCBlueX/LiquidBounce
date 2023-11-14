package net.ccbluex.liquidbounce.script.bindings.api

/**
 * The main hub of the ScriptAPI that provides access to all kind of useful APIs.
 */
object ApiProvider {

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
    val item = JsItemUtil

}
