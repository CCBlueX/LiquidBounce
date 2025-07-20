package net.ccbluex.liquidbounce.utils.io

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import net.minecraft.util.math.Vec3d

fun Vec3d.toJson(): JsonElement {
    return JsonArray(3).apply {
        add(x)
        add(y)
        add(z)
    }
}
