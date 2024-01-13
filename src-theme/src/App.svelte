<script>
    import Router, { link, pop, push } from "svelte-spa-router";
    import { routes } from "./routes.js";
    import { listenAlways, cleanupListeners } from "./client/ws.svelte";
    import { getVirtualScreen } from "./client/api.svelte";
    import { insertPersistentData } from "./client/persistentStorage.svelte";

    insertPersistentData();

    let showingSplash = false;
    let nextRoute = null;

    // Check if the URL has a STATIC tag http://127.0.0.1/#/hud?static
    const url = window.location.href;
    const staticTag = url.split("?")[1];
    const isStatic = staticTag === "static";

    listenAlways("splashOverlay", function (event) {
        const action = event.action;

        if (isStatic) {
            return;
        }

        if (action === "show") {
            cleanupListeners();
            push("/");
            showingSplash = true;
        } else if (action === "hide") {
            cleanupListeners();
            if (nextRoute != null) {
                push(nextRoute);
            } else {
                push("/closed");
            }

            showingSplash = false;
            nextRoute = null;
        }
    });

    listenAlways("virtualScreen", function (event) {
        const screenName = event.screenName;
        const action = event.action;

        if (isStatic) {
            return;
        }

        if (action === "close") {
            cleanupListeners();
            push("/closed");
        } else if (action === "open") {
            const route = "/" + screenName;

            if (showingSplash) {
                nextRoute = route;
            } else {
                cleanupListeners();
                push(route);
            }
        }
    });

    if (!isStatic) {
        getVirtualScreen().then((screen) => {
            const screenName = screen.name;
            const route = "/" + screenName;

            if (screen.splash) {
                showingSplash = true;
                nextRoute = route;
            } else {
                cleanupListeners();
                push(route);
            }
        });
    }
</script>

<Router {routes}/>
