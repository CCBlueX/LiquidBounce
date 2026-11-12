<script lang="ts">
    import type {Component} from "svelte";
    import {onMount} from "svelte";
    import {fade} from "svelte/transition";
    import type {ConfigurableSetting} from "../../integration/types";
    import {getClientInfo, getGameWindow, getModuleSettings, setHudEditorSelected, setTyping} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {ClickGuiValueChangeEvent, ScaleFactorChangeEvent} from "../../integration/events";
    import {persistentDataLoaded, setItem} from "../../integration/persistent_storage";
    import {isTextEntry} from "../../util/utils";
    import {darken, os, scaleFactor} from "./clickgui_store";
    import AreaTabs from "./AreaTabs.svelte";
    import ScaledClickGuiContent from "./ScaledClickGuiContent.svelte";
    import Workspace from "./Workspace.svelte";
    import Modules from "./tabs/modules/Modules.svelte";
    import ModuleSearch from "./tabs/modules/ModuleSearch.svelte";
    import GlobalSettings from "./tabs/GlobalSettings.svelte";
    import HudEditor from "./tabs/hud_editor/HudEditor.svelte";
    import Marketplace from "./tabs/marketplace/Marketplace.svelte";

    const AREA_KEY = "clickgui.tab";

    // The HUD editor works on the game view, every other area lives in the workspace window
    const areas: { title: string; content: Component; search?: Component }[] = [
        {title: "Modules", content: Modules, search: ModuleSearch},
        {title: "Marketplace", content: Marketplace},
        {title: "Settings", content: GlobalSettings},
        {title: "HUD Editor", content: HudEditor},
    ];
    const titles = areas.map(area => area.title);

    let active = $state(0);
    let restored = false;
    let minecraftScaleFactor = $state(2);
    let clickGuiScaleFactor = $state(1);

    const area = $derived(areas[active]);
    const inWindow = $derived(area.content !== HudEditor);

    $effect(() => {
        $scaleFactor = minecraftScaleFactor * clickGuiScaleFactor;
    });

    $effect(() => {
        const title = area.title;
        if (restored) {
            setItem(AREA_KEY, title);
        }
    });

    async function restoreArea() {
        await persistentDataLoaded;
        const stored = titles.indexOf(localStorage.getItem(AREA_KEY) ?? "");
        if (stored >= 0) {
            active = stored;
        }
        restored = true;
    }

    /**
     * The client has to know whether the user is typing, otherwise key presses
     * that are meant for a text field are also handled as game input - see
     * `ModuleClickGui.isInSearchBar`.
     *
     * Focus events bubble, so tracking them here covers every text field of the
     * ClickGUI, including the ones rendered by the HUD editor.
     */
    async function handleFocusIn(event: FocusEvent) {
        await setTyping(isTextEntry(event.target));
    }

    async function handleFocusOut(event: FocusEvent) {
        // `relatedTarget` is the element that receives the focus, so moving
        // from one text field to another does not report a pause in typing.
        await setTyping(isTextEntry(event.relatedTarget));
    }

    function applyValues(configurable: ConfigurableSetting) {
        const scaleValue = configurable.value.find(v => v.name === "Scale");
        if (scaleValue) {
            clickGuiScaleFactor = scaleValue.value as number;
        }
    }

    onMount(async () => {
        await setHudEditorSelected(false);
        restoreArea();

        $os = (await getClientInfo()).os;

        const gameWindow = await getGameWindow();
        minecraftScaleFactor = gameWindow.scaleFactor;

        const clickGuiSettings = await getModuleSettings("ClickGUI");
        applyValues(clickGuiSettings);

        await setTyping(false);
    });

    listen("scaleFactorChange", (e: ScaleFactorChangeEvent) => {
        minecraftScaleFactor = e.scaleFactor;
    });

    listen("clickGuiValueChange", (e: ClickGuiValueChangeEvent) => {
        applyValues(e.configurable);
    });
</script>

<svelte:window onfocusin={handleFocusIn} onfocusout={handleFocusOut}/>

<div class="clickgui" class:darken={$darken}>
    {#if inWindow}
        <ScaledClickGuiContent>
            <Workspace>
                {#snippet tabs()}
                    <AreaTabs {titles} bind:active/>
                {/snippet}
                {#snippet search()}
                    {#if area.search}
                        <area.search/>
                    {/if}
                {/snippet}

                {#key area}
                    <div class="area" in:fade={{duration: 120, delay: 40}} out:fade={{duration: 50}}>
                        <area.content/>
                    </div>
                {/key}
            </Workspace>
        </ScaledClickGuiContent>
    {:else}
        <div class="floating-tabs" transition:fade={{duration: 150}}>
            <AreaTabs {titles} bind:active/>
        </div>
        <HudEditor/>
    {/if}
</div>

<style lang="scss">
  .clickgui {
    overflow: hidden;
    position: absolute;
    inset: 0;
    transition: ease background-color .2s;

    &.darken {
      background-color: var(--clickgui-overlay-background-color);
    }
  }

  // Both the leaving and the entering area are in the window for a moment
  .area {
    grid-area: 1 / 1;
    display: flex;
    min-width: 0;
    min-height: 0;
  }

  .floating-tabs {
    position: fixed;
    top: 15px;
    left: 50%;
    transform: translateX(-50%);
    border-radius: 999px;
    background-color: var(--clickgui-tabs-background-color);
    box-shadow: 0 0 10px var(--clickgui-tabs-shadow-color);
    z-index: 9999999999;
  }
</style>
