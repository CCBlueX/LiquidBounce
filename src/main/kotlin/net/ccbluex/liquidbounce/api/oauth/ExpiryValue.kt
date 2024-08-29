package net.ccbluex.liquidbounce.api.oauth

/**
 * Represents a value that expires at a certain time defined by [expiresAt].
 */
data class ExpiryValue<T>(val value: T, val expiresAt: Long) {
    fun isExpired() = expiresAt < System.currentTimeMillis()
    override fun toString() = value.toString()
}
