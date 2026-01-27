<script lang="ts">
    import {onMount} from "svelte";
    import {
        getClientInfo,
        getGameWindow,
        getModules,
        getModuleSettings,
        getModuleTagGroups,
        setTyping
    } from "../../integration/rest";
    import {filterModulesByTags, groupByCategory} from "../../integration/util";
    import type {
        ConfigurableSetting,
        GroupedModules,
        Module,
        ModuleTagGroups,
        TogglableSetting
    } from "../../integration/types";
    import Panel from "./Panel.svelte";
    import Search from "./Search.svelte";
    import Description from "./Description.svelte";
    import TagFilter from "./TagFilter.svelte";
    import {fade} from "svelte/transition";
    import {listen} from "../../integration/ws";
    import type {ClickGuiValueChangeEvent, ScaleFactorChangeEvent} from "../../integration/events";
    import {gridSize, os, scaleFactor, selectedModuleTags, showGrid, snappingEnabled} from "./clickgui_store";

    let categories: GroupedModules = {};
    let modules: Module[] = [];
    let tagGroups: ModuleTagGroups = [];
    let tagDefaultsApplied = false;
    let minecraftScaleFactor = 2;
    let clickGuiScaleFactor = 1;
    $: {
        scaleFactor.set(minecraftScaleFactor * clickGuiScaleFactor);
    }

    function applyValues(configurable: ConfigurableSetting) {
        clickGuiScaleFactor = configurable.value.find(v => v.name === "Scale")?.value as number ?? 1;

        const snappingValue = configurable.value.find(v => v.name === "Snapping") as TogglableSetting;

        $snappingEnabled = snappingValue?.value.find(v => v.name === "Enabled")?.value as boolean ?? true;
        $gridSize = snappingValue?.value.find(v => v.name === "GridSize")?.value as number ?? 10;
    }

    onMount(async () => {
        getClientInfo().then(info => os.set(info.os));

        const gameWindow = await getGameWindow();
        minecraftScaleFactor = gameWindow.scaleFactor;

        modules = await getModules();
        tagGroups = await getModuleTagGroups();

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

    $: categories = groupByCategory(filterModulesByTags(modules, $selectedModuleTags, tagGroups));
    $: if (!tagDefaultsApplied && tagGroups.length > 0) {
        const requiredTags = tagGroups
            .filter((group) => group.required)
            .flatMap((group) => group.tags);
        const defaultTags = requiredTags.length > 0 ? requiredTags : tagGroups.flatMap((group) => group.tags);
        selectedModuleTags.set(defaultTags);
        tagDefaultsApplied = true;
    }
</script>

<div class="clickgui" class:grid={$showGrid} transition:fade|global={{duration: 200}}
     style="transform: scale({$scaleFactor * 50}%); width: {2 / $scaleFactor * 100}vw; height: {2 / $scaleFactor * 100}vh;
     background-size: {$gridSize}px {$gridSize}px;">
    <Description/>
    <Search modules={structuredClone(modules)}/>
    <TagFilter groups={tagGroups}/>

    {#each Object.entries(categories) as [category, modules], panelIndex}
        <Panel {category} {modules} {panelIndex}/>
    {/each}
</div>

<style lang="scss">
  @use "../../colors.scss" as *;

  $GRID_SIZE: 10px;

  .clickgui {
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
