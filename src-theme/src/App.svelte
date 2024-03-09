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
    import AltManager from "./routes/menu/altmanager/AltManager.svelte";
    import Singleplayer from "./routes/menu/singleplayer/Singleplayer.svelte";
    import ProxyManager from "./routes/menu/proxymanager/ProxyManager.svelte";

    const routes = {
        "/clickgui": ClickGui,
        "/hud": Hud,
        "/inventory": Inventory,
        "/title": Title,
        "/splash": SplashScreen,
        "/multiplayer": Multiplayer,
        "/altmanager": AltManager,
        "/singleplayer": Singleplayer,
        "/proxymanager": ProxyManager
    };

    const url = window.location.href;
    const staticTag = url.split("?")[1];
    const isStatic = staticTag === "static";
    let showSplash = false;

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

        listenAlways("splashOverlay", async (event: any) => {
            setTimeout(() => {
                showSplash = event.showingSplash;
            }, 3000);
        });

        listenAlways("virtualScreen", async (event: any) => {
            console.log(`[Router] Virtual screen change to ${event.screenName}`)
            const action = event.action;

            switch (action) {
                case "close":
                    await changeRoute("none");
                    break;
                case "open":
                    await changeRoute(event.screenName || "none");
                    break;
            }
        });

        const virtualScreen = await getVirtualScreen();
        await changeRoute(virtualScreen.name || "none");
        showSplash = virtualScreen.showingSplash;
    });
</script>

<main>
    {#if showSplash}
        <SplashScreen/>
    {:else}
        <Router {routes}/>
    {/if}
</main>
