<script>
    import Router, {push} from "svelte-spa-router";
    import {routes} from "./routes.js";
    import {cleanupListeners, listenAlways} from "./client/ws.svelte";
    import {confirmVirtualScreen, getVirtualScreen} from "./client/api.svelte";
    import {insertPersistentData} from "./client/persistentStorage.svelte";
    import Splashscreen from "./routes/splashscreen/Splashscreen.svelte";

    insertPersistentData();

    let showingSplash = false;

    // Check if the URL has a STATIC tag http://127.0.0.1/#/hud?static
    const url = window.location.href;
    const staticTag = url.split("?")[1];
    const isStatic = staticTag === "static";

    function changeRoute(name) {
        confirmVirtualScreen(name);
        cleanupListeners();

        push("/" + name).then(() => {
            console.log("[Router] Changed to: " + name);
        }).catch(console.error);
    }

    if (!isStatic) {
        listenAlways("splashOverlay", function (event) {
            showingSplash = event.showingSplash;
            console.log("[Splash] Showing: " + showingSplash);
        });

        listenAlways("virtualScreen", function (event) {
            const action = event.action;

            switch (action) {
                case "close":
                    changeRoute("none");
                    break;
                case "open":
                    changeRoute(event.screenName || "none");
                    break;
            }
        });

        getVirtualScreen().then((screen) => {
            const screenName = screen.name || "none";
            changeRoute(screenName, screen.splash);

            showingSplash = screen.showingSplash;
            console.log("[Splash] Showing: " + showingSplash);
        });
    }
</script>

<main>
    {#if showingSplash}
        <Splashscreen></Splashscreen>
    {:else}
        <Router {routes} />
    {/if}
</main>
