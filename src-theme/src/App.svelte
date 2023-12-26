<script>
    import Router, { link, pop, push } from "svelte-spa-router";
    import { routes } from "./routes.js";
    import { listen } from "./client/ws.svelte";
    import { getVirtualScreen } from "./client/api.svelte";
    import { insertPersistentData } from "./client/persistentStorage.svelte";

    insertPersistentData();

    let showingSplash = false;
    let nextRoute = null;

    // Check if the URL has a STATIC tag http://127.0.0.1/#/hud?static
    const url = window.location.href;
    const staticTag = url.split("?")[1];
    const isStatic = staticTag === "static";

    listen("splashOverlay", function (event) {
        const action = event.action;

        if (isStatic) {
            return;
        }

        console.log("splashOverlay", action);
        if (action === "show") {
            push("/");
            showingSplash = true;
        } else if (action === "hide" && nextRoute != null) {
            push(nextRoute);
            showingSplash = false;
            nextRoute = null;
        }
    });

    listen("virtualScreen", function (event) {
        const screenName = event.screenName;
        const action = event.action;

        if (isStatic) {
            return;
        }

        if (action === "close") {
            push("/closed");
        } else if (action === "open") {
            const route = "/" + screenName;

            if (showingSplash) {
                nextRoute = route;
            } else {
                push(route);
            }
        }
    });

    if (!isStatic) {
        getVirtualScreen().then((screen) => {
            push("/" + screen.name);
        });
    }
    
</script>

<main>
    <Router {routes}/>
</main>
