<script lang="ts">
    import {onMount, onDestroy} from "svelte";
    import {getClientInfo, getModules, getModuleSettings, setTyping} from "../../integration/rest";
    import {groupByCategory} from "../../integration/util";
    import Panel from "./Panel.svelte";
    import Description from "./Description.svelte";
    import {fade} from "svelte/transition";
    import {listen} from "../../integration/ws";
    import Search from './Search.svelte';
    import {ResolutionScaler} from "../../util/ResolutionScaler"

    import {
        os,
        gridSize,
        showGrid,
        snappingEnabled,
        scaleFactor,
        showSearch,
        panelLength,
        fontSize,
        moduleAutoCollapse,
        moduleDescription
    } from "./clickgui_store";
    import type {
        ClickGuiValueChangeEvent,
    } from "../../integration/events";
    import type {

        ConfigurableSetting,
        GroupedModules,
        Module,
        TogglableSetting
    } from "../../integration/types";
    import CtrlFTip from "./CtrlFTip.svelte";


    let resolutionScaler = new ResolutionScaler({
        baseResolution: {width: 1920, height: 1080}
    });
    let categories: GroupedModules = {};
    let modules: Module[] = [];
    let minecraftScaleFactor = 2;
    let clickGuiScaleFactor = 1;
    let panelLengthFactor = 1
    let ClickGuiFontSizeFactor = 14
    $: {
        scaleFactor.set(minecraftScaleFactor * clickGuiScaleFactor * resolutionScaler.getScaleFactor());
        panelLength.set(panelLengthFactor)
        fontSize.set(ClickGuiFontSizeFactor)
    }

    const applyValues = (configurable: ConfigurableSetting) => {

        clickGuiScaleFactor = configurable.value.find(v => v.name === "Scale")?.value as number ?? 1;
        panelLengthFactor = configurable.value.find(v => v.name === "Length")?.value as number ?? 66
        ClickGuiFontSizeFactor = configurable.value.find(v => v.name === "FontSize")?.value as number ?? 14
        const snappingValue = configurable.value.find(v => v.name === "Snapping") as TogglableSetting;
        $moduleDescription = configurable.value.find(v => v.name === "Description")?.value as boolean ?? false;
        $moduleAutoCollapse = configurable.value.find(v => v.name === "ModuleAutoCollapse")?.value as boolean ?? true;
        $snappingEnabled = snappingValue?.value.find(v => v.name === "Enabled")?.value as boolean ?? true;
        $gridSize = snappingValue?.value.find(v => v.name === "GridSize")?.value as number ?? 10;

    };
    fontSize.subscribe((fontSize) => {
        if (typeof document === 'undefined') return;
        document.documentElement.style.setProperty('--font-size', `${fontSize}px`);
    });

    const handleResize = () => {
        requestAnimationFrame(() => {
            resolutionScaler.updateScaleFactor();
            scaleFactor.set(minecraftScaleFactor * clickGuiScaleFactor * resolutionScaler.getScaleFactor());

        });
    };


    onMount(async () => {
        const info = await getClientInfo();
        os.set(info.os);
        resolutionScaler.updateScaleFactor();
        scaleFactor.set(minecraftScaleFactor * clickGuiScaleFactor * resolutionScaler.getScaleFactor());
        panelLength.set(panelLengthFactor)
        modules = await getModules();
        categories = groupByCategory(modules);

        const clickGuiSettings = await getModuleSettings("ClickGUI");
        applyValues(clickGuiSettings);

        await setTyping(false);


        window.addEventListener("resize", handleResize);

    });

    onDestroy(() => {
        window.removeEventListener("resize", handleResize);
    });


    listen("clickGuiValueChange", (e: ClickGuiValueChangeEvent) => {
        applyValues(e.configurable);
    });
</script>
<div class="clickgui" class:grid={$showGrid}
     style="transform: scale({$scaleFactor * 50}%); width: {2 / $scaleFactor * 100}vw; height: {2 / $scaleFactor * 100}vh;
     background-size: {$gridSize}px {$gridSize}px;"
     transition:fade|global={{duration: 200}}>

    <CtrlFTip showSearch={showSearch}/>

    {#if $moduleDescription}
        <Description/>
    {/if}
    <Search modules={structuredClone(modules)}/>


    {#each Object.entries(categories) as [category, modules], panelIndex}
        <Panel {category} {modules} {panelIndex}/>
    {/each}
</div>


<style lang="scss">
  @use "../../colors.scss" as *;

  $GRID_SIZE: 10px;

  .clickgui {
    overflow: hidden;
    position: absolute;
    will-change: opacity;
    top: 0;
    left: 0;
    transform-origin: left top;

    &.grid {
      background-image: linear-gradient(to right, rgba($clickgui-grid-color, 0.3) 1px, transparent 1px),
      linear-gradient(to bottom, rgba($clickgui-grid-color, 0.3) 1px, transparent 1px);
      background-size: #{$GRID_SIZE}px #{$GRID_SIZE}px;
    }
  }

</style>
