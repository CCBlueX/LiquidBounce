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

    export function redirect(path) {
        window.location.href = BASE_URL + "/" + path
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

    export function getModules() {
        return request("/modules")
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

