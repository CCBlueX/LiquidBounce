<script lang="ts">
    import Router, {push} from "svelte-spa-router";
    import ClickGui from "./routes/clickgui/ClickGui.svelte";
    import Hud from "./routes/hud/Hud.svelte";
    import {getVirtualScreen} from "./integration/rest";
    import {cleanupListeners, listenAlways} from "./integration/ws";
    import {onMount} from "svelte";
    import {insertPersistentData} from "./integration/persistent_storage";
    import Title from "./routes/menu/title/Title.svelte";
    import Multiplayer from "./routes/menu/multiplayer/Multiplayer.svelte";
    import AltManager from "./routes/menu/altmanager/AltManager.svelte";
    import Singleplayer from "./routes/menu/singleplayer/Singleplayer.svelte";
    import ProxyManager from "./routes/menu/proxymanager/ProxyManager.svelte";
    import Disconnected from "./routes/menu/disconnected/Disconnected.svelte";
    import Browser from "./routes/browser/Browser.svelte";
    import LoginMenu from "./routes/menu/login/LoginMenu.svelte";
    import LockScreen from "./routes/menu/login/LockScreen.svelte";
    import ConnectionScreen from "./routes/menu/connected/ConnectionScreen.svelte";
    import SpareModule from "./routes/menu/common/SpareModule.svelte";
    import Inventory from "./routes/menu/inventory/Inventory.svelte";
    import ConfigManager from "./routes/clickgui/ConfigManager.svelte";
    import None from "./routes/menu/none/None.svelte";

    const routes = {
        "/loginmenu": LoginMenu,
        "/lockscreen": LockScreen,
        "/clickgui": ClickGui,
        "/hud": Hud,
        "/layouteditor": Hud,
        "/inventory": Inventory,
        "/game_menu": ConfigManager,
        "/chest": SpareModule,
        "/title": Title,
        "/multiplayer": Multiplayer,
        "/altmanager": AltManager,
        "/singleplayer": Singleplayer,
        "/proxymanager": ProxyManager,
        "/disconnected": Disconnected,
        "/connecting": ConnectionScreen,
        "/browser": Browser,
        "/none": None,
    };

    const url = window.location.href;
    const staticTag = url.split("?")[1];
    const isStatic = staticTag === "static";

    async function changeRoute(name: string) {
        cleanupListeners();
        console.log(`[Router] Redirecting to ${name}`);
        await push(`/${name}`);
    }


    onMount(async () => {

        await insertPersistentData();

        if (isStatic) {
            return;
        }

        listenAlways("socketReady", async () => {
            const virtualScreen = await getVirtualScreen();
            await changeRoute(virtualScreen.name || "none");
        });

        listenAlways("virtualScreen", async (event: any) => {
            console.log(`[Router] Virtual screen change to ${event.screenName}`);
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
    });
</script>

<main>
    <Router {routes}/>
</main>
