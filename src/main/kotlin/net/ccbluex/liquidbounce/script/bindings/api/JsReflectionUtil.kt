package net.ccbluex.liquidbounce.script.bindings.api

object JsReflectionUtil {

    @JvmName("classByName")
    fun classByName(name: String): Class<*> = Class.forName(name)

    @JvmName("classByObject")
    fun classByObject(obj: Any): Class<*> = obj::class.java

    @JvmName("newInstance")
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any? =
        clazz.getDeclaredConstructor(*args.map { it!!::class.java }.toTypedArray()).apply {
            isAccessible = true
        }.newInstance(*args)

    @JvmName("newInstanceByName")
    fun newInstanceByName(name: String, vararg args: Any?): Any? =
        Class.forName(name).getDeclaredConstructor(*args.map { it!!::class.java }.toTypedArray()).apply {
            isAccessible = true
        }.newInstance(*args)

    @JvmName("newInstanceByObject")
    fun newInstanceByObject(obj: Any, vararg args: Any?): Any? =
        obj::class.java.getDeclaredConstructor(*args.map { it!!::class.java }.toTypedArray()).apply {
            isAccessible = true
        }.newInstance(*args)

    @JvmName("takeField")
    fun takeField(obj: Any, name: String): Any? = obj::class.java.getDeclaredField(name).apply {
        isAccessible = true
    }.get(obj)

    @JvmName("takeStaticField")
    fun takeStaticField(clazz: Class<*>, name: String): Any? = clazz.getDeclaredField(name).apply {
        isAccessible = true
    }.get(null)

    @JvmName("invokeMethod")
    fun invokeMethod(obj: Any, name: String, vararg args: Any?): Any? =
        obj::class.java.getDeclaredMethod(name, *args.map { it!!::class.java }.toTypedArray()).apply {
            isAccessible = true
        }.invoke(obj, *args)

    @JvmName("invokeStaticMethod")
    fun invokeStaticMethod(clazz: Class<*>, name: String, vararg args: Any?): Any? =
        clazz.getDeclaredMethod(name, *args.map { it!!::class.java }.toTypedArray()).apply {
            isAccessible = true
        }.invoke(null, *args)

}
