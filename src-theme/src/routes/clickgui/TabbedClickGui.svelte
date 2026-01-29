<script lang="ts">
    import ClickGui from "./ClickGui.svelte";
    import GlobalSettings from "./tabs/GlobalSettings.svelte";
    import Tabs from "./tabs/Tabs.svelte";
    import {gridSize, os, scaleFactor, showGrid, snappingEnabled} from "./clickgui_store";
    import type {ConfigurableSetting, TogglableSetting} from "../../integration/types";
    import {onMount} from "svelte";
    import {getClientInfo, getGameWindow, getModuleSettings, setTyping} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {ClickGuiValueChangeEvent, ScaleFactorChangeEvent} from "../../integration/events";

    const tabs = [
        {title: "ClickGUI", content: ClickGui},
        {title: "Settings", content: GlobalSettings}
    ];

    let activeTab = $state(0);
    let minecraftScaleFactor = $state(2);
    let clickGuiScaleFactor = $state(1);

    $effect(() => {
        $scaleFactor = minecraftScaleFactor * clickGuiScaleFactor;
    });

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

<div
        class="tabbed-clickgui"
        class:grid={$showGrid}
        style="
    transform: scale({$scaleFactor * 50}%);
    width: {2 / $scaleFactor * 100}vw;
    height: {2 / $scaleFactor * 100}vh;
    background-size: {$gridSize}px {$gridSize}px;
  "
>
    <Tabs {tabs} bind:activeTab/>
</div>

<style lang="scss">
  @use "../../colors.scss" as *;

  $GRID_SIZE: 10px;

  .tabbed-clickgui {
    background-color: rgba($clickgui-base-color, 0.6);
    overflow: hidden;
    position: absolute;
    will-change: opacity;
    transform-origin: top left;
    left: 0;
    top: 0;

    &.grid {
      background-image: linear-gradient(to right, $clickgui-grid-color 1px, transparent 1px),
      linear-gradient(to bottom, $clickgui-grid-color 1px, transparent 1px);
    }
  }
</style>
