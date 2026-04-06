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
    import type {Metadata, Theme} from "./integration/types";

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

    const ACCENT_THEME_COLOR_NAME = "Accent";
    const TINT_THEME_COLOR_NAME = "Tint";
    const SURFACE_TINT_MIX = 18;

    type FoundationColors = {
        "accent-color": string;
        "surface-color": string;
    };

    let foundationColors: FoundationColors | null = null;
    let defaultAccentColor = "";
    let defaultTintColor = "#000000";
    let currentAccentColor = "";
    let currentTintColor = "#000000";

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

    function getFoundationColors() {
        if (foundationColors !== null) {
            return foundationColors;
        }

        foundationColors = {
            "accent-color": getThemeColor("accent-color"),
            "surface-color": getThemeColor("surface-color")
        };

        return foundationColors;
    }

    function themeColorToHex(value: number) {
        return rgbaToHex(intToRgba(value));
    }


    function getThemeColorValue(theme: Theme, name: string, fallback: string) {
        const value = theme.colors[name.toLowerCase()];

        if (value === undefined) {
            return fallback;
        }

        return themeColorToHex(value);
    }

    function isThemeColorName(actualName: string, expectedName: string) {
        return actualName.toLowerCase() === expectedName.toLowerCase();
    }

    function getMetadataColorValue(metadata: Metadata, name: string, fallback: string) {
        return metadata.colors?.[name] ?? fallback;
    }

    function getTintedSurfaceColor(tintColor: string) {
        const surfaceColor = getFoundationColors()["surface-color"];
        return `color-mix(in srgb, ${surfaceColor} ${100 - SURFACE_TINT_MIX}%, ${tintColor})`;
    }

    function applyThemeColors(accentColor: string, tintColor: string) {
        currentAccentColor = accentColor;
        currentTintColor = tintColor;

        setThemeColor("accent-color", accentColor);
        setThemeColor("surface-color", getTintedSurfaceColor(tintColor));
    }

    async function applyColors(id: string) {
        const theme = await getTheme(id);
        const accentColor = getThemeColorValue(theme, ACCENT_THEME_COLOR_NAME, defaultAccentColor);
        const tintColor = getThemeColorValue(theme, TINT_THEME_COLOR_NAME, defaultTintColor);

        applyThemeColors(accentColor, tintColor);
    }

    onMount(async () => {
        const colors = getFoundationColors();
        let metadata = await getMetadata();
        defaultAccentColor = getMetadataColorValue(metadata, ACCENT_THEME_COLOR_NAME, colors["accent-color"]);
        defaultTintColor = getMetadataColorValue(metadata, TINT_THEME_COLOR_NAME, defaultTintColor);

        await applyColors(metadata.id);
        await insertPersistentData();

        listenAlways("themeColorChange", async (event: ThemeColorChangeEvent) => {
            if (event.themeId !== metadata.id) {
                return;
            }

            if (isThemeColorName(event.name, ACCENT_THEME_COLOR_NAME)) {
                applyThemeColors(themeColorToHex(event.value), currentTintColor);
                return;
            }

            if (isThemeColorName(event.name, TINT_THEME_COLOR_NAME)) {
                applyThemeColors(currentAccentColor, themeColorToHex(event.value));
                return;
            }

            await applyColors(metadata.id);
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
