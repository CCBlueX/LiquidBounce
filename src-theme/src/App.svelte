<script>
    import Router, { link, pop, push } from "svelte-spa-router";
    import { routes } from "./routes.js";
    import { listenAlways, cleanupListeners } from "./client/ws.svelte";
    import { getVirtualScreen, confirmVirtualScreen } from "./client/api.svelte";
    import { insertPersistentData } from "./client/persistentStorage.svelte";

    insertPersistentData();

    let showingSplash = false;
    let nextRoute = null;

    // Check if the URL has a STATIC tag http://127.0.0.1/#/hud?static
    const url = window.location.href;
    const staticTag = url.split("?")[1];
    const isStatic = staticTag === "static";

    function changeRoute(name, splash = false) {
        confirmVirtualScreen(name);

        if (splash) {
            showingSplash = true;
            nextRoute = name;
        } else {
            cleanupListeners();
            push("/" + name);

            showingSplash = false;
            nextRoute = null;
        }
    }

    if (!isStatic) {
        listenAlways("splashOverlay", function (event) {
            const action = event.action;

            if (action === "show") {
                cleanupListeners();
                push("/");
                showingSplash = true;
            } else if (action === "hide") {
                changeRoute(nextRoute || "none");
            }
        });

        listenAlways("virtualScreen", function (event) {
            const action = event.action;

            switch (action) {
                case "close":
                    changeRoute("none");
                    break;
                case "open":
                    const screenName = event.screenName || "none";
                    changeRoute(screenName, showingSplash);
                    break;
            }
        });

        getVirtualScreen().then((screen) => {
            const screenName = screen.name || "none";
            changeRoute(screenName, screen.splash);
        });
    }
</script>

<Router {routes}/>
