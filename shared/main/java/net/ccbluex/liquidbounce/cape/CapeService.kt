/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.cape

import java.util.*

interface CapeService {

    /**
     * @param uuid of user
     * @return url of cape
     */
    fun getCape(uuid: UUID): String?

}

class ServiceAPI(private val baseURL: String) : CapeService {

    /**
     * @param uuid of user
     * @return url of cape
     */
    override fun getCape(uuid: UUID) = String.format(baseURL, uuid)

}

class ServiceList(private val users: Map<String, String>) : CapeService {

    /**
     * @param uuid of user
     * @return url of cape
     */
    override fun getCape(uuid: UUID) = users[uuid.toString().replace("-", "")]

}
