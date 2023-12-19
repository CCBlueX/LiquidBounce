<script>
    import Router, { link, pop, push } from "svelte-spa-router";
    import { routes } from "./routes.js";
    import { listen } from "./client/ws.svelte";

    let showingSplash = false;
    let nextRoute = null;

    listen("splashOverlay", function (event) {
        const action = event.action;

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
</script>

<main>
    <Router {routes}/>
</main>
