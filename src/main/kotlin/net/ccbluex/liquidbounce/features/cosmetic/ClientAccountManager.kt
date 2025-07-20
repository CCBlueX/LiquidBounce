package net.ccbluex.liquidbounce.features.cosmetic

import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.config.types.Configurable

object ClientAccountManager : Configurable("account") {
    var clientAccount by value("account", ClientAccount.EMPTY_ACCOUNT)
}
