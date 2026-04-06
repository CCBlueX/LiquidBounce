<script lang="ts">
    import Router, {push} from "svelte-spa-router";
    import Hud from "./routes/hud/Hud.svelte";
    import {getMetadata, getTheme, getVirtualScreen} from "./integration/rest";
    import {cleanupListeners, listenAlways} from "./integration/ws";
    import {onMount} from "svelte";
    import {insertPersistentData} from "./integration/persistent_storage";
    import {isStatic} from "./integration/host";
    import Inventory from "./routes/inventory/Inventory.svelte";
    import Title from "./routes/menu/title/Title.svelte";
    import Multiplayer from "./routes/menu/multiplayer/Multiplayer.svelte";
    import AltManager from "./routes/menu/altmanager/AltManager.svelte";
    import Singleplayer from "./routes/menu/singleplayer/Singleplayer.svelte";
    import ProxyManager from "./routes/menu/proxymanager/ProxyManager.svelte";
    import None from "./routes/none/None.svelte";
    import Disconnected from "./routes/menu/disconnected/Disconnected.svelte";
    import Browser from "./routes/browser/Browser.svelte";
    import TabbedClickGui from "./routes/clickgui/TabbedClickGui.svelte";
    import {intToRgba, rgbaToHex} from "./integration/util";
    import type {ThemeColorChangeEvent} from "./integration/events";

    const routes = {
        "/clickgui": TabbedClickGui,
        "/hud": Hud,
        "/inventory": Inventory,
        "/title": Title,
        "/multiplayer": Multiplayer,
        "/altmanager": AltManager,
        "/singleplayer": Singleplayer,
        "/proxymanager": ProxyManager,
        "/none": None,
        "/disconnected": Disconnected,
        "/browser": Browser
    };

    const foundationColorMixes = {
        "surface-color": 18,
        "text-color": 8,
        "text-dimmed-color": 14
    } as const;

    const foundationColorNames = [
        "accent-color",
        "surface-color",
        "text-color",
        "text-dimmed-color"
    ] as const;

    type FoundationColorName = keyof typeof foundationColorMixes;
    type FoundationColorVariable = typeof foundationColorNames[number];
    type FoundationColors = Record<FoundationColorVariable, string>;

    let foundationColors: FoundationColors | null = null;

    async function changeRoute(name: string) {
        cleanupListeners();
        console.log(`[Router] Redirecting to ${name}`);
        await push(`/${name}`);
    }

    function setThemeColor(name: string, value: string) {
        document.documentElement.style.setProperty(`--${name}`, value);
    }

    function getThemeColor(name: string) {
        return getComputedStyle(document.documentElement).getPropertyValue(`--${name}`).trim();
    }

    function readDefaultFoundationColors(): FoundationColors {
        const rootStyle = document.documentElement.style;
        const inlineOverrides = new Map<FoundationColorVariable, string>();

        for (const name of foundationColorNames) {
            inlineOverrides.set(name, rootStyle.getPropertyValue(`--${name}`));
            rootStyle.removeProperty(`--${name}`);
        }

        try {
            return {
                "accent-color": getThemeColor("accent-color"),
                "surface-color": getThemeColor("surface-color"),
                "text-color": getThemeColor("text-color"),
                "text-dimmed-color": getThemeColor("text-dimmed-color")
            };
        } finally {
            for (const name of foundationColorNames) {
                const value = inlineOverrides.get(name);

                if (value) {
                    rootStyle.setProperty(`--${name}`, value);
                    continue;
                }

                rootStyle.removeProperty(`--${name}`);
            }
        }
    }

    function getFoundationColors() {
        if (foundationColors !== null) {
            return foundationColors;
        }

        foundationColors = readDefaultFoundationColors();

        return foundationColors;
    }

    function themeColorToHex(value: number) {
        return rgbaToHex(intToRgba(value));
    }

    function getTintedFoundationColor(name: FoundationColorName, accentColor: string) {
        const colors = getFoundationColors();

        if (accentColor === colors["accent-color"]) {
            return colors[name];
        }

        const accentMix = foundationColorMixes[name];
        return `color-mix(in srgb, ${colors[name]} ${100 - accentMix}%, ${accentColor})`;
    }

    function applyAccentTint(accentColor: string) {
        setThemeColor("accent-color", accentColor);
        setThemeColor("surface-color", getTintedFoundationColor("surface-color", accentColor));
        setThemeColor("text-color", getTintedFoundationColor("text-color", accentColor));
        setThemeColor("text-dimmed-color", getTintedFoundationColor("text-dimmed-color", accentColor));
    }

    async function applyColors(id: string) {
        let theme = await getTheme(id);
        let accentValue = Object.values(theme.colors)[0];
        let accentColor = accentValue === undefined
            ? getFoundationColors()["accent-color"]
            : themeColorToHex(accentValue);
        applyAccentTint(accentColor);
    }

    onMount(async () => {
        getFoundationColors();

        let metadata = await getMetadata();

        await applyColors(metadata.id);
        await insertPersistentData();

        listenAlways("themeColorChange", async (event: ThemeColorChangeEvent) => {
            console.log(JSON.stringify(event));
            if (event.themeId !== metadata.id) {
                return;
            }

            applyAccentTint(themeColorToHex(event.value));
        });

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
