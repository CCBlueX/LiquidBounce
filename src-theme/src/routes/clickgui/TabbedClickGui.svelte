<script lang="ts">
    import ClickGui from "./ClickGui.svelte";
    import GlobalSettings from "./tabs/GlobalSettings.svelte";
    import Tabs from "./tabs/Tabs.svelte";
    import {gridSize, os, scaleFactor, snappingEnabled, darken} from "./clickgui_store";
    import type {ConfigurableSetting, TogglableSetting} from "../../integration/types";
    import {onMount} from "svelte";
    import {
        getClientInfo,
        getGameWindow,
        getModuleSettings,
        setHudEditorSelected,
        setTyping
    } from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {ClickGuiValueChangeEvent, ScaleFactorChangeEvent} from "../../integration/events";
    import HudEditor from "./tabs/hud_editor/HudEditor.svelte";
    import {isTextEntry} from "../../util/utils";

    const tabs = [
        {title: "ClickGUI", content: ClickGui},
        {title: "HUD Editor", content: HudEditor},
        {title: "Settings", content: GlobalSettings},
    ];

    let activeTab = $state(0);
    let minecraftScaleFactor = $state(2);
    let clickGuiScaleFactor = $state(1);

    $effect(() => {
        $scaleFactor = minecraftScaleFactor * clickGuiScaleFactor;
    });

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
        const snappingValue = configurable.value.find(v => v.name === "Snapping") as TogglableSetting | undefined;

        if (scaleValue) {
            clickGuiScaleFactor = scaleValue.value as number;
        }

        if (snappingValue) {
            $snappingEnabled = snappingValue.value.find(v => v.name === "Enabled")?.value as boolean ?? true;
            $gridSize = snappingValue.value.find(v => v.name === "GridSize")?.value as number ?? 10;
        }
    }

    onMount(async () => {
        await setHudEditorSelected(false);

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

<div
        class="tabbed-clickgui"
        class:darken={$darken}
>
    <Tabs {tabs} bind:activeTab/>
</div>

<style lang="scss">
  .tabbed-clickgui {
    overflow: hidden;
    position: absolute;
    inset: 0;
    transition: ease background-color .2s;

    &.darken {
      background-color: var(--clickgui-overlay-background-color);
    }
  }
</style>
