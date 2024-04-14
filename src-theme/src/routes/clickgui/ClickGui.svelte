<script lang="ts">
    import {onMount} from "svelte";
    import {getComponents, getGameWindow, getModules} from "../../integration/rest";
    import {groupByCategory} from "../../integration/util";
    import type {GroupedModules, Module} from "../../integration/types";
    import Panel from "./Panel.svelte";
    import Search from "./Search.svelte";
    import Description from "./Description.svelte";
    import {fade} from "svelte/transition";
    import {listen} from "../../integration/ws";
    import type {ScaleFactorChangeEvent} from "../../integration/events";

    let categories: GroupedModules = {};
    let modules: Module[] = [];
    let scaleFactor = 2;
    $: zoom = scaleFactor * 50;

    onMount(async () => {
        const gameWindow = await getGameWindow();
        scaleFactor = gameWindow.scaleFactor;

        modules = await getModules();
        categories = groupByCategory(modules);
    });

    listen("scaleFactorChange", (data: ScaleFactorChangeEvent) => {
        scaleFactor = data.scaleFactor;
    });
</script>

<div class="clickgui" transition:fade|global={{duration: 200}}
     style="zoom: {zoom}%; width: {2 / scaleFactor * 100}vw; height: {2 / scaleFactor * 100}vh;">
    <Description/>
    <Search modules={structuredClone(modules)}/>

    {#each Object.entries(categories) as [category, modules], panelIndex}
        <Panel {category} {modules} {panelIndex} {scaleFactor}/>
    {/each}
</div>

<style lang="scss">
  @import "../../colors.scss";

  .clickgui {
    background-color: rgba($clickgui-base-color, 0.6);
    overflow: hidden;
    position: relative;
    will-change: opacity;
  }
</style>
