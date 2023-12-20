<script context="module">
    // todo: use window location instead
    const BASE_URL = "http://127.0.0.1:15743" // window.location.href.replace(/\/$/, "")
    // 127.0.0.1:1337/api/v1/client
    const BASE_API_URL = BASE_URL + "/api/v1/client"
    console.log("BASE_API_URL: " + BASE_API_URL);

    async function request(path, options) {
        const response = await fetch(`${BASE_API_URL}${path}`, {
            ...options
        })
        const data = await response.json()

        if (!response.ok) {
            if (data.reason) throw new Error(data.reason)

            throw new Error("An unknown error occurred")
        }

        return data
    }

    /**
     * get("/exit") {
     *         mc.scheduleStop()
     *         httpOk(JsonObject())
     *     }
     */
    export function exitClient() {
        return request("/exit")
    }

    /**
     *
     * @param url
     * @returns {Promise<any>}
     */
    export function browse(url) {
        return request("/browse", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ "url": url })
        })
    }

    /**
     *     put("/screen") {
     *         val body = decode<JsonObject>(it.content)
     *         val screenName = body["name"]?.asString ?: return@put httpForbidden("No screen name")
     *
     *         IntegrationHandler.VirtualScreenType.values().find { it.assignedName == screenName }?.open()
     *             ?: return@put httpForbidden("No screen with name $screenName")
     *         httpOk(JsonObject())
     *     }
     *
     * @param name
     * @returns {Promise<any>}
     */
    export function openScreen(name) {
        return request("/screen", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ "name": name })
        })
    }

    /**
     * get("/session") {
     *         httpOk(JsonObject().apply {
     *             mc.session.let {
     *                 addProperty("username", it.username)
     *                 addProperty("uuid", it.uuidOrNull.toString())
     *                 addProperty("accountType", it.accountType.getName())
     *                 addProperty("faceUrl", ClientApi.FACE_URL.format(mc.session.uuidOrNull))
     *                 addProperty("premium", it.isPremium())
     *             }
     *         })
     *     }

     * @returns {Promise<any>}
     */
    export function getSession() {
        return request("/session")
    }

    /**
     *     get("/location") {
     *         httpOk(
     *             protocolGson.toJsonTree(
     *                 IpInfoApi.localIpInfo ?: return@get httpForbidden("location is not known (yet)")
     *             )
     *         )
     *     }
     * @returns {Promise<any>}
     */
    export function getLocation() {
        return request("/location")
    }

    export function getWorlds() {
        return request("/worlds")
    }

    export async function getServers() {
        return request("/servers")
    }

    export function getLocalPlayer() {
        return request("/player")
    }

    export function getCategories() {
        return request("/categories")
    }

    export function toggleModule(name, enabled) {
        return request("/modules/toggle", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ "name": name, "enabled": enabled })
        })
    }

    export function getModules() {
        return request("/modules")
    }

    /**
     * get("/accounts") {
     *         val accounts = JsonArray()
     *         for ((i, account) in AccountManager.accounts.withIndex()) {
     *             accounts.add(JsonObject().apply {
     *                 // Why are we not serializing the whole account?
     *                 // -> We do not want to share the access token or anything relevant
     *
     *                 addProperty("id", i)
     *                 addProperty("username", account.profile?.username)
     *                 addProperty("uuid", account.profile?.uuid.toString())
     *                 addProperty("faceUrl", ClientApi.FACE_URL.format(account.profile?.uuid))
     *                 addProperty("type", account.type)
     *             })
     *         }
     *         httpOk(accounts)
     *     }
     * @param name
     */
    export function getAccounts() {
        return request("/accounts")
    }

    /**
     * post("/account/login") {
     *         class AccountForm(
     *             val id: Int
     *         )
     *         val accountForm = decode<AccountForm>(it.content)
     *         AccountManager.loginAccount(accountForm.id)
     *
     *         httpOk(JsonObject().apply {
     *             mc.session.let {
     *                 addProperty("username", it.username)
     *                 addProperty("uuid", it.uuidOrNull.toString())
     *                 addProperty("accountType", it.accountType.getName())
     *                 addProperty("faceUrl", ClientApi.FACE_URL.format(mc.session.uuidOrNull))
     *                 addProperty("premium", it.isPremium())
     *             }
     *         })
     *     }
     * @param id
     */
    export function loginAccount(id) {
        return request("/account/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ "id": id })
        })
    }

    /**
     * post("/accounts/new/cracked") {
     *         class AccountForm(
     *             val username: String
     *         )
     *         val accountForm = decode<AccountForm>(it.content)
     *
     *         AccountManager.newCrackedAccount(accountForm.username)
     *         httpOk(JsonObject())
     *     }
     * @param username
     */
    export function newCrackedAccount(username) {
        return request("/accounts/new/cracked", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ "username": username })
        })
    }

    /**
     * post("/accounts/new/microsoft") {
     *         AccountManager.newMicrosoftAccount()
     *         httpOk(JsonObject())
     *     }
     */
    export function newMicrosoftAccount() {
        return request("/accounts/new/microsoft", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            }
        })
    }

    /**
     *         class AlteningForm(
     *             val token: String
     *         )
     *         val accountForm = decode<AlteningForm>(it.content)
     *         AccountManager.newAlteningAccount(accountForm.token)
     *         httpOk(JsonObject())
     * @param token
     */
    export function newAltening(token) {
        return request("/accounts/new/altening", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ "token": token })
        })
    }

    /**
     *         class AlteningGenForm(
     *             val apiToken: String
     *         )
     *         val accountForm = decode<AlteningGenForm>(it.content)
     *
     *         AccountManager.generateAlteningAccount(accountForm.apiToken)
     *         httpOk(JsonObject())
     * @param apiToken
     */
    export function newAlteningGen(apiToken) {
        return request("/accounts/new/alteningGenerate", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ "apiToken": apiToken })
        })
    }

    /**
     * // Deletes a specific account
     *     delete("/account") {
     *         class AccountForm(
     *             val id: Int
     *         )
     *
     *         val accountForm = decode<AccountForm>(it.content)
     *         AccountManager.accounts.removeAt(accountForm.id)
     *         httpOk(JsonObject())
     *     }
     * @param name
     */
    export function deleteAccount(id) {
        return request("/account", {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ "id": id })
        })
    }

    export function restoreInitialAccount() {
        return request("/account/restoreInitial", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            }
        })
    }

    export function getModuleSettings(name) {
        const searchParams = new URLSearchParams({ name })

        return request("/modules/settings?" + searchParams.toString(), {
            headers: {
                "Content-Type": "application/json"
            }
        })
    }


</script>

