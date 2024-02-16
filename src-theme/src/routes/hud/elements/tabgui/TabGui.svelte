<script lang="ts">
    import { onMount } from "svelte";
    import type {
        GroupedModules,
        Module as TModule,
    } from "../../../../integration/types";
    import { getModules } from "../../../../integration/rest";
    import { groupByCategory } from "../../../../integration/util";
    import Category from "./Category.svelte";
    import { getTextWidth } from "../../../../integration/text_measurement";
    import ModuleList from "./ModuleList.svelte";
    import { listen } from "../../../../integration/ws";

    let modules: TModule[] = [];
    let groupedModules: GroupedModules = {};
    let categories: string[] = [];
    let selectedCategoryIndex = 0;
    let renderedModules: TModule[] = [];
    let categoriesElement: HTMLElement;

    onMount(async () => {
        modules = (await getModules()).filter((m) => m.category !== "Client");
        groupedModules = groupByCategory(modules);
        categories = Object.keys(groupedModules).sort(
            (a, b) =>
                getTextWidth(b, "Inter 14px") - getTextWidth(a, "Inter 14px"),
        );
    });

    function handleKeyDown(e: KeyboardEvent) {
        switch (e.key) {
            case "ArrowDown":
                if (renderedModules.length > 0) {
                    return;
                }
                selectedCategoryIndex =
                    (selectedCategoryIndex + 1) %
                    Object.keys(categories).length;
                break;
            case "ArrowUp":
                if (renderedModules.length > 0) {
                    return;
                }
                selectedCategoryIndex =
                    (selectedCategoryIndex -
                        1 +
                        Object.keys(categories).length) %
                    Object.keys(categories).length;
                break;
            case "ArrowLeft":
                renderedModules = [];
                break;
            case "ArrowRight":
                if (renderedModules.length > 0) {
                    return;
                }
                renderedModules =
                    groupedModules[categories[selectedCategoryIndex]];
                break;
        }
    }

    listen("toggleModule", (e: any) => {
        const moduleName = e.moduleName;
        const moduleEnabled = e.enabled;

        const mod = modules.find((m) => m.name === moduleName);
        if (!mod) return;

        mod.enabled = moduleEnabled;
        groupedModules = groupByCategory(modules);
        if (renderedModules.length > 0) {
            renderedModules = groupedModules[categories[selectedCategoryIndex]];
        }
    });
</script>

<svelte:window on:keydown={handleKeyDown} />

<div class="tabgui">
    <div class="categories" bind:this={categoriesElement}>
        {#each categories as name, index}
            <Category {name} selected={index === selectedCategoryIndex} />
        {/each}
    </div>

    {#if renderedModules.length > 0}
        <ModuleList
            modules={renderedModules}
            height={categoriesElement.offsetHeight}
        />
    {/if}
</div>

<style lang="scss">
    @import "../../../../colors.scss";

    .tabgui {
        position: fixed;
        top: 90px;
        left: 15px;
        display: flex;
    }

    .categories {
        background-clip: content-box;
        display: flex;
        flex-direction: column;
        border-radius: 5px;
        overflow: hidden;
    }
</style>
