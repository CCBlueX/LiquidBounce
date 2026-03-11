<script lang="ts">
    import type {GroupedModules, Module} from "../../integration/types";
    import Panel from "./Panel.svelte";
    import Search from "./Search.svelte";
    import Description from "./Description.svelte";
    import {fade} from "svelte/transition";
    import {onMount} from "svelte";
    import {getModules} from "../../integration/rest";
    import {groupByCategory} from "../../integration/util";

    let categories = $state<GroupedModules>({});
    let modules = $state<Module[]>([]);

    onMount(async () => {
        modules = await getModules();
        categories = groupByCategory(modules);
    });
</script>

<div class="clickgui" transition:fade|global={{ duration: 200 }}>
    <Description/>
    <Search modules={structuredClone($state.snapshot(modules))}/>

    {#each Object.entries(categories) as [category, modules], panelIndex (category)}
        <Panel {category} {modules} {panelIndex}/>
    {/each}
</div>
