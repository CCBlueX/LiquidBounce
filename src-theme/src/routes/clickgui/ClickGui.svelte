<script lang="ts">
    import {onMount} from "svelte";
    import {getModules} from "../../integration/rest";
    import {groupByCategory} from "../../integration/util";
    import type {GroupedModules, Module} from "../../integration/types";
    import Panel from "./Panel.svelte";
    import Search from "./Search.svelte";
    import Description from "./Description.svelte";
    import {fade} from "svelte/transition";

    let categories: GroupedModules = {};
    let modules: Module[] = [];
    let highlightModuleName = "";

    onMount(async () => {
        modules = await getModules();
        categories = groupByCategory(modules);
    });
</script>

<div class="clickgui" transition:fade|global={{duration: 200}}>
    <Description />
    <Search modules={structuredClone(modules)}/>

    {#each Object.entries(categories) as [category, modules], panelIndex}
        <Panel {category} {modules} {panelIndex}/>
    {/each}
</div>

<style lang="scss">
  @import "../../colors.scss";

  .clickgui {
    height: 100vh;
    width: 100vw;
    max-width: 100vw;
    max-height: 100vh;
    background-color: rgba($clickgui-base-color, 0.6);
    overflow: hidden;
    position: relative;
    will-change: opacity;
  }
</style>
