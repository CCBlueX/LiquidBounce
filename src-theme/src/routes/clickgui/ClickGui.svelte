<script lang="ts">
    import type {GroupedModules, Module} from "../../integration/types";
    import Panel from "./Panel.svelte";
    import Search from "./Search.svelte";
    import Description from "./Description.svelte";
    import ClientPicker from "./ClientPicker.svelte";
    import {fade} from "svelte/transition";
    import {getCategories, getModules} from "../../integration/rest";
    import {groupByCategory} from "../../integration/util";
    import {gridSize, pickedClient, showGrid} from "./clickgui_store";
    import ScaledClickGuiContent from "./ScaledClickGuiContent.svelte";

    let categories = $state<GroupedModules>({});
    let modules = $state<Module[]>([]);
    let icons = $state<Record<string, string>>({});

    async function loadModules(client: number | null) {
        const loaded = await getModules(client);
        if (client !== $pickedClient) {
            return;
        }

        // Modules that serve the window are shared, so another client has none of its own
        modules = loaded.filter(module => client === null || module.perClient);
        categories = groupByCategory(modules);
    }

    $effect(() => {
        loadModules($pickedClient);
    });

    getCategories().then(categories => {
        icons = Object.fromEntries(
            categories
                .filter(category => category.icon)
                .map(category => [category.name, category.icon!])
        );
    });
</script>

<ScaledClickGuiContent>
    <div
            class="clickgui"
            class:grid={$showGrid}
            style="background-size: {$gridSize}px {$gridSize}px;"
            transition:fade|global={{duration: 200}}
    >
        <Description/>
        {#key modules}
            <Search modules={structuredClone($state.snapshot(modules))}/>

            {#each Object.entries(categories) as [category, modules], panelIndex (category)}
                <Panel {category} {modules} {panelIndex} icon={icons[category]}/>
            {/each}
        {/key}
    </div>
</ScaledClickGuiContent>

<ClientPicker/>

<style lang="scss">
  .clickgui {
    position: absolute;
    inset: 0;

    &.grid {
      background-image: linear-gradient(to right, var(--clickgui-grid-color) 1px, transparent 1px),
      linear-gradient(to bottom, var(--clickgui-grid-color) 1px, transparent 1px);
    }
  }
</style>
