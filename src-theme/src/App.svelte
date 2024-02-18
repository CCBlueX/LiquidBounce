<script lang="ts">
    import Router, {push} from "svelte-spa-router";
    import ClickGui from "./routes/clickgui/ClickGui.svelte";
    import Hud from "./routes/hud/Hud.svelte";
    import {confirmVirtualScreen, getVirtualScreen} from "./integration/rest";
    import {cleanupListeners, listenAlways} from "./integration/ws";
    import {onMount} from "svelte";
    import {insertPersistentData} from "./integration/persistent_storage";
    import Inventory from "./routes/inventory/Inventory.svelte";

    const routes = {
        "/clickgui": ClickGui,
        "/hud": Hud,
        "/inventory": Inventory
    };

    const url = window.location.href;
    const staticTag = url.split("?")[1];
    const isStatic = staticTag === "static";
    let showingSplash = false;
    let nextRoute: string | null = null;

    async function changeRoute(name: string, splash = false) {
        confirmVirtualScreen(name);

        if (splash) {
            showingSplash = true;
            nextRoute = name;
        } else {
            cleanupListeners();
            await push("/" + name);
            showingSplash = false;
            nextRoute = null;
        }
    }

    onMount(async () => {
        await insertPersistentData();

        if (isStatic) {
            return;
        }

        listenAlways("splashOverlay", async (event: any) => {
            const action = event.action;

            if (action === "show") {
                cleanupListeners();
                await push("/");

                showingSplash = true;
            } else if (action === "hide") {
                await changeRoute(nextRoute ?? "none");
            }
        });

        listenAlways("virtualScreen", async (event: any) => {
            const action = event.action;

            switch (action) {
                case "close":
                    await changeRoute("none");
                    break;
                case "open":
                    const screenName = event.screenName ?? "none";
                    await changeRoute(screenName, showingSplash);
                    break;
            }
        });

        const screen = await getVirtualScreen();

        const screenName = screen.name ?? "none";
        changeRoute(screenName, screen.splash);
    });
</script>

<main>
    <Router {routes} />
</main>
