package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata

data class DataSample(
    val timestamp: Long,
    val playerId: String,
    val posX: Double,
    val posY: Double,
    val posZ: Double,
    val yaw: Float,
    val pitch: Float,
    val isSneaking: Boolean,
    val isOnGround: Boolean,
    val isUsingItem: Boolean,
    val isSwinging: Boolean,
    val wasHit: Boolean,
    val closestArrowX: Double, // relative to player
    val closestArrowY: Double, // relative to player
    val closestArrowZ: Double, // relative to player
    val mainHandCategory: Int,
    val offHandCategory: Int,
    val floorMap: ShortArray, // 15x15 = 225
    val ceilMap: ShortArray, // 15x15 = 225
    val poiMap: IntArray // 15x15 = 225
)
