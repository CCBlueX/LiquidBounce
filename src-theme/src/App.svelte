<script lang="ts">
    import Router, {push} from "svelte-spa-router";
    import ClickGui from "./routes/clickgui/ClickGui.svelte";
    import Hud from "./routes/hud/Hud.svelte";
    import {confirmVirtualScreen, getVirtualScreen} from "./integration/rest";
    import {cleanupListeners, listenAlways} from "./integration/ws";
    import {onMount} from "svelte";
    import {insertPersistentData} from "./integration/persistent_storage";
    import Inventory from "./routes/inventory/Inventory.svelte";
    import Title from "./routes/menu/title/Title.svelte";
    import SplashScreen from "./routes/menu/splash/SplashScreen.svelte";
    import Multiplayer from "./routes/menu/multiplayer/Multiplayer.svelte";

    const routes = {
        "/clickgui": ClickGui,
        "/hud": Hud,
        "/inventory": Inventory,
        "/title": Title,
        "/splash": SplashScreen,
        "/multiplayer": Multiplayer
    };

    const url = window.location.href;
    const staticTag = url.split("?")[1];
    const isStatic = staticTag === "static";

    async function changeRoute(name: string) {
        console.log(`[Router] Redirecting to ${name}`);
        await confirmVirtualScreen(name);

        cleanupListeners();
        await push(`/${name}`);
    }

    onMount(async () => {
        await insertPersistentData();

        if (isStatic) {
            return;
        }

        listenAlways("virtualScreen", async (event: any) => {
            console.log(`[Router] Virtual screen change to ${event.screenName}`)
            const action = event.action;

            switch (action) {
                case "close":
                    await changeRoute("none");
                    break;
                case "open":
                    const screenName = event.screenName;
                    if (screenName) {
                        await changeRoute(screenName);
                    }
                    break;
            }
        });
    });
</script>

<main>
    <Router {routes}/>
</main>
